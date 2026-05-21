function LoadingIndicator({ label = '불러오는 중입니다' }) {
  return (
    <div className="loading-indicator" aria-live="polite">
      <span className="loading-dot" />
      <span>{label}</span>
    </div>
  );
}

export default LoadingIndicator;
