import PrimaryButton from './PrimaryButton';

export default function Modal({
  title,
  children,
  onClose,
  primaryLabel,
  onPrimary,
  secondaryLabel,
  onSecondary,
}) {
  return (
    <div className="modal-backdrop">
      <div className="modal">
        <header className="modal-header">
          <h3>{title}</h3>
          <PrimaryButton type="button" variant="ghost" onClick={onClose}>
            A-
          </PrimaryButton>
        </header>
        <div className="modal-body">{children}</div>
        {(primaryLabel || secondaryLabel) && (
          <div className="modal-actions">
            {secondaryLabel && (
              <PrimaryButton
                type="button"
                variant="secondary"
                onClick={onSecondary || onClose}
              >
                {secondaryLabel}
              </PrimaryButton>
            )}
            {primaryLabel && (
              <PrimaryButton type="button" onClick={onPrimary}>
                {primaryLabel}
              </PrimaryButton>
            )}
          </div>
        )}
      </div>
    </div>
  );
}
