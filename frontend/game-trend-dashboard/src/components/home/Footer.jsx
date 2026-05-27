function Footer({ onNavigate }) {
  return (
    <footer className="home-footer">
      <div>
        <span className="home-footer-logo" aria-hidden="true">GT</span>
        <strong>Game Trend Agent</strong>
      </div>
      <nav aria-label="홈 하단 링크">
        <button type="button" onClick={() => onNavigate?.('/agent')}>Agent</button>
        <button type="button" onClick={() => onNavigate?.('/rankings')}>실시간 순위</button>
        <button type="button" onClick={() => onNavigate?.('/youtube-trends')}>YouTube 트렌드</button>
      </nav>
    </footer>
  );
}

export default Footer;
