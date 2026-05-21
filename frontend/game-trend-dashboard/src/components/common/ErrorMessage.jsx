function ErrorMessage({ message, onClose }) {
  if (!message) {
    return null;
  }

  return (
    <div className="error-message" role="alert">
      <div>
        <strong>요청 실패</strong>
        <span>{message}</span>
      </div>
      <button type="button" onClick={onClose} aria-label="오류 메시지 닫기">
        닫기
      </button>
    </div>
  );
}

export default ErrorMessage;
