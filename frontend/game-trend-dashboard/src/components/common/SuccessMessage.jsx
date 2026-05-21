function SuccessMessage({ message, onClose }) {
  if (!message) {
    return null;
  }

  return (
    <div className="success-message" role="status">
      <div>
        <strong>완료</strong>
        <span>{message}</span>
      </div>
      <button type="button" onClick={onClose} aria-label="성공 메시지 닫기">
        닫기
      </button>
    </div>
  );
}

export default SuccessMessage;
