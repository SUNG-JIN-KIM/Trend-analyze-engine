import { getScoreTone } from '../../utils/score.js';

function clampScore(value) {
  const number = Number(value);
  if (Number.isNaN(number)) {
    return 0;
  }
  return Math.min(100, Math.max(0, number));
}

function ScoreControl({ label, value, onChange }) {
  const tone = getScoreTone(value);

  const handleChange = (event) => {
    onChange(clampScore(event.target.value));
  };

  return (
    <label className={`score-control score-${tone}`}>
      <div className="score-control-top">
        <span>{label}</span>
        <strong>{value}</strong>
      </div>
      <input
        type="range"
        min="0"
        max="100"
        value={value}
        onChange={handleChange}
      />
      <input
        className="score-number"
        type="number"
        min="0"
        max="100"
        value={value}
        onChange={handleChange}
      />
    </label>
  );
}

export default ScoreControl;
