import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { TinderSkuDeck } from '../src/components/tinder-sku-deck';

// Mock fetch globally
global.fetch = jest.fn() as jest.Mock;

describe('TinderSkuDeck Component (SKU Merging UX)', () => {
  const mockCandidate = {
    skuAId: 10,
    skuAName: 'Áo thun đỏ',
    skuBId: 11,
    skuBName: 'Ao thun do',
    distance: 2
  };

  beforeEach(() => {
    jest.clearAllMocks();
    // Default mock for loading candidates
    (global.fetch as jest.Mock).mockImplementation((url: string) => {
      if (url.includes('/candidates')) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve([mockCandidate]),
        });
      }
      return Promise.resolve({ ok: true, text: () => Promise.resolve('') });
    });
  });

  it('should render candidate and call merge API when user clicks "Gộp chung"', async () => {
    render(<TinderSkuDeck />);

    // Wait for data to load
    await waitFor(() => {
      expect(screen.getByText(/\[Áo thun đỏ\]/i)).toBeInTheDocument();
    });

    const mergeButton = screen.getByRole('button', { name: /Gộp chung/i });
    fireEvent.click(mergeButton);

    // Verify API call
    await waitFor(() => {
      expect(global.fetch).toHaveBeenCalledWith(
        expect.stringContaining('/sku-merge/merge'),
        expect.objectContaining({
          method: 'POST',
          body: JSON.stringify({ parentSkuId: 10, childSkuId: 11 }),
        })
      );
    });
  });

  it('should skip candidate without API call when user clicks "Bỏ qua"', async () => {
    render(<TinderSkuDeck />);

    await waitFor(() => {
      expect(screen.getByText(/\[Áo thun đỏ\]/i)).toBeInTheDocument();
    });

    const skipButton = screen.getByRole('button', { name: /Bỏ qua/i });
    fireEvent.click(skipButton);

    // Should remove card from UI (show empty message since we only had 1 candidate)
    await waitFor(() => {
      expect(screen.getByText(/Không còn cặp gợi ý/i)).toBeInTheDocument();
    });

    // Verify NO merge API call was made
    const mergeCalls = (global.fetch as jest.Mock).mock.calls.filter(c => c[0].includes('/merge'));
    expect(mergeCalls.length).toBe(0);
  });
});
