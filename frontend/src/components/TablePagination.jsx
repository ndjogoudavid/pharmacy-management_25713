import PropTypes from 'prop-types';
import PrimaryButton from './PrimaryButton';

export default function TablePagination({ page = 0, totalPages = 1, onPageChange }) {
  const safeTotal = Math.max(totalPages || 0, 1);
  const canPrev = page > 0;
  const canNext = page + 1 < safeTotal;

  const goTo = (next) => {
    if (onPageChange) {
      onPageChange(Math.min(Math.max(next, 0), safeTotal - 1));
    }
  };

  return (
    <div className="pagination">
      <PrimaryButton
        type="button"
        variant="secondary"
        disabled={!canPrev}
        onClick={() => goTo(page - 1)}
      >
        Prev
      </PrimaryButton>
      <span>
        Page {page + 1} of {safeTotal}
      </span>
      <PrimaryButton
        type="button"
        variant="secondary"
        disabled={!canNext}
        onClick={() => goTo(page + 1)}
      >
        Next
      </PrimaryButton>
    </div>
  );
}

TablePagination.propTypes = {
  page: PropTypes.number,
  totalPages: PropTypes.number,
  onPageChange: PropTypes.func.isRequired,
};
