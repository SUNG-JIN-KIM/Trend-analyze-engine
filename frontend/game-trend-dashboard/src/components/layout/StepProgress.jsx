function StepProgress({ currentStep, labels }) {
  if (currentStep === 0) {
    return null;
  }

  const steps = labels.slice(1);
  const activeIndex = currentStep - 1;
  const progress = Math.round((currentStep / steps.length) * 100);

  return (
    <div className="step-progress" aria-label="온보딩 진행률">
      <div className="step-progress-top">
        <span>{labels[currentStep]}</span>
        <span>Step {currentStep} / {steps.length}</span>
      </div>
      <div className="step-indicators">
        {steps.map((label, index) => (
          <div
            className={`step-indicator ${index === activeIndex ? 'active' : ''} ${index < activeIndex ? 'done' : ''}`}
            key={label}
          >
            <span>{index + 1}</span>
            <strong>{label}</strong>
          </div>
        ))}
      </div>
      <div className="progress-track">
        <div className="progress-fill" style={{ width: `${progress}%` }} />
      </div>
    </div>
  );
}

export default StepProgress;
