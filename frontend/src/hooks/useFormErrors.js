// Hook to help render server-side field errors returned as { fieldErrors: { field: 'message' } }
export default function useFormErrors(fieldErrors) {
  const getFieldError = (field) => {
    if (!fieldErrors) return null;
    return fieldErrors[field] || null;
  };

  const hasErrors = () => fieldErrors && Object.keys(fieldErrors).length > 0;

  return { getFieldError, hasErrors };
}
