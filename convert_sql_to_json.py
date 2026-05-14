import re
import json
import os

sql_file = r'c:\NDT\ndt\Du_An\supplyforge-ai\test-data\Database - NCKH\products.sql'
json_file = r'c:\NDT\ndt\Du_An\supplyforge-ai\test-data\Database - NCKH\products.json'

print(f"Reading {sql_file}...")

with open(sql_file, 'r', encoding='utf-8') as f:
    content = f.read()

# Tìm phần INSERT INTO `products` (...) VALUES
# Regex để bắt các bản ghi dạng ('...', '...', ...)
# Lưu ý: Cấu trúc file dump có 38 cột
print("Parsing SQL records...")
records = []
# Tìm tất cả các cụm ( ... ) sau VALUES
# Dùng regex cẩn thận với chuỗi chứa dấu phẩy bên trong
pattern = re.compile(r"\((?P<vals>.*)\)[,;]")
matches = pattern.finditer(content)

# Danh sách các cột dựa trên schema trong file SQL
columns = [
    "_id", "userId", "categoryId", "metaTitle", "metaDesciption", "metaOgImage",
    "displayType", "activeOnWeb", "data", "images", "weight", "brand",
    "customDescription", "description", "descriptionInfo", "descriptionType",
    "thirdPartyId", "itemId", "productName", "platform", "productImage",
    "itemSku", "price", "priceOnWebsite", "listedPrice", "stock", "cost",
    "totalRevenue", "totalProfit", "totalOrders", "lifeTimeDate", "isBoostItem",
    "extraGiftId", "points", "giftId", "guaranteeId", "createdAt", "updatedAt"
]

def split_sql_values(val_str):
    # Một hàm parser đơn giản để tách các giá trị trong SQL (xử lý dấu phẩy trong chuỗi)
    vals = []
    current = []
    in_string = False
    escape = False
    for char in val_str:
        if escape:
            current.append(char)
            escape = False
        elif char == '\\':
            current.append(char)
            escape = True
        elif char == "'":
            in_string = not in_string
            current.append(char)
        elif char == ',' and not in_string:
            vals.append("".join(current).strip())
            current = []
        else:
            current.append(char)
    vals.append("".join(current).strip())
    return vals

count = 0
for match in matches:
    val_str = match.group('vals')
    # Tách các dòng VALUES nếu có nhiều dòng trong 1 lệnh INSERT
    # Trong dump của phpMyAdmin thường là 1 lệnh INSERT lớn
    # Chúng ta cần tách thủ công các hàng (row)
    # Một Row kết thúc bằng ),(
    rows = re.split(r"\),\s*\(", val_str)
    for row in rows:
        raw_values = split_sql_values(row)
        if len(raw_values) == len(columns):
            item = {}
            for i, col in enumerate(columns):
                val = raw_values[i]
                if val.upper() == 'NULL':
                    item[col] = None
                elif val.startswith("'") and val.endswith("'"):
                    val = val[1:-1].replace("''", "'") # Unescape SQL
                    # Xử lý nếu là JSON string
                    if (val.startswith('{') and val.endswith('}')) or (val.startswith('[') and val.endswith(']')):
                        try:
                            item[col] = json.loads(val)
                        except:
                            item[col] = val
                    else:
                        item[col] = val
                else:
                    try:
                        if '.' in val:
                            item[col] = float(val)
                        else:
                            item[col] = int(val)
                    except:
                        item[col] = val
            records.append(item)
            count += 1
            if count % 1000 == 0:
                print(f"Processed {count} records...")

print(f"Writing {count} records to {json_file}...")
with open(json_file, 'w', encoding='utf-8') as f:
    json.dump(records, f, ensure_ascii=False, indent=2)

print("Done!")
