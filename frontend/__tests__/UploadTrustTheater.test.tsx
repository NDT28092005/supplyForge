import { render, screen, fireEvent, waitFor, act } from '@testing-library/react';
import { TrustTheaterUpload } from '../src/components/trust-theater-upload';

// Mock the API call globally
global.fetch = jest.fn(() =>
  Promise.resolve({
    ok: true,
    json: () => Promise.resolve({ rowsImported: 100, dataSourceId: 1 }),
  })
) as jest.Mock;

describe('TrustTheaterUpload Component (E2E UX)', () => {
  beforeEach(() => {
    jest.useFakeTimers();
    (global.fetch as jest.Mock).mockClear();
    
    // Mock performance.now() for predictable behavior
    let now = 0;
    jest.spyOn(performance, 'now').mockImplementation(() => now);
    global.advanceTime = (ms: number) => {
      now += ms;
      jest.advanceTimersByTime(ms);
    };
  });

  afterEach(() => {
    jest.useRealTimers();
  });

  it('should render trust animation steps and complete upload', async () => {
    render(<TrustTheaterUpload />);

    const dropzone = screen.getByTestId('upload-dropzone');
    const dummyFile = new File(['dummy content'], 'inventory.xlsx', { 
        type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' 
    });
    
    fireEvent.drop(dropzone, {
      dataTransfer: {
        files: [dummyFile],
      },
    });

    // Verify initial state
    expect(screen.getByText(/Đọc file nội bộ/i)).toBeInTheDocument();

    // Fast forward through all animations and timeouts (3 steps * 480ms + minIntroMs)
    // Cần lặp nhiều lần để các 'await setTimeout' trong loop của component có thể chạy tiếp
    for (let i = 0; i < 10; i++) {
        await act(async () => {
            // @ts-ignore
            global.advanceTime(500);
        });
    }

    // Check final result
    await waitFor(() => {
      // Dùng hàm matcher vì text bị chia bởi tag <strong>
      expect(screen.getByText((content) => content.includes('Đã nhập'))).toBeInTheDocument();
      expect(screen.getByText('100')).toBeInTheDocument();
    });
    
    expect(global.fetch).toHaveBeenCalledTimes(1);
  });
});
