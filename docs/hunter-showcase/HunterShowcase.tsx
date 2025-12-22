import React from 'react';
import './HunterShowcase.css';

const HunterShowcase: React.FC = () => {
  return (
    <div className="hunter-showcase">
      {/* Background Effects */}
      <div className="bg-particles"></div>
      <div className="bg-gradient"></div>

      {/* Header */}
      <header className="header">
        <div className="title-container">
          <span className="class-icon">🏹</span>
          <h1 className="title">CHASSEUR</h1>
          <span className="class-icon">🏹</span>
        </div>
        <p className="subtitle">MAÎTRE DE LA CHASSE</p>
        <p className="tagline">"Chaque flèche trouve sa proie. Chaque proie trouve sa fin."</p>
      </header>

      {/* Stats Section */}
      <section className="stats-section">
        <div className="stat stat-positive">
          <span className="stat-icon">⚔️</span>
          <span className="stat-label">Dégâts</span>
          <span className="stat-value positive">+20%</span>
        </div>
        <div className="stat stat-positive">
          <span className="stat-icon">💨</span>
          <span className="stat-label">Vitesse</span>
          <span className="stat-value positive">+20%</span>
        </div>
        <div className="stat stat-positive">
          <span className="stat-icon">🎯</span>
          <span className="stat-label">Critique</span>
          <span className="stat-value positive">+35%</span>
        </div>
        <div className="stat stat-negative">
          <span className="stat-icon">❤️</span>
          <span className="stat-label">Vie</span>
          <span className="stat-value negative">-15%</span>
        </div>
      </section>

      {/* Divider */}
      <div className="divider">
        <span className="divider-text">5 VOIES DE SPÉCIALISATION</span>
      </div>

      {/* Specialization Paths */}
      <section className="paths-section">
        <div className="path path-barrage">
          <div className="path-icon-wrapper">
            <span className="path-icon">🌧️</span>
          </div>
          <h3 className="path-name">BARRAGE</h3>
          <p className="path-desc">Pluies de flèches dévastatrices</p>
          <div className="path-talents">
            <span className="talent-tag">Multi-tirs</span>
            <span className="talent-tag">Déluge</span>
            <span className="talent-tag legendary">Frappe Orbitale</span>
          </div>
        </div>

        <div className="path path-beasts">
          <div className="path-icon-wrapper">
            <span className="path-icon">🐺</span>
          </div>
          <h3 className="path-name">BÊTES</h3>
          <p className="path-desc">Invocations de compagnons</p>
          <div className="path-talents">
            <span className="talent-tag">Loup</span>
            <span className="talent-tag">Renard</span>
            <span className="talent-tag legendary">Golem de Fer</span>
          </div>
        </div>

        <div className="path path-shadow">
          <div className="path-icon-wrapper">
            <span className="path-icon">🌑</span>
          </div>
          <h3 className="path-name">OMBRE</h3>
          <p className="path-desc">Assassinat & Exécutions</p>
          <div className="path-talents">
            <span className="talent-tag">Pas de l'Ombre</span>
            <span className="talent-tag">Exécution</span>
            <span className="talent-tag legendary">Avatar d'Ombre</span>
          </div>
        </div>

        <div className="path path-poison">
          <div className="path-icon-wrapper">
            <span className="path-icon">☠️</span>
          </div>
          <h3 className="path-name">POISON</h3>
          <p className="path-desc">Corruption progressive DoT</p>
          <div className="path-talents">
            <span className="talent-tag">Virulence</span>
            <span className="talent-tag">Pandémie</span>
            <span className="talent-tag legendary">Avatar de la Peste</span>
          </div>
        </div>

        <div className="path path-frost">
          <div className="path-icon-wrapper">
            <span className="path-icon">❄️</span>
          </div>
          <h3 className="path-name">GIVRE</h3>
          <p className="path-desc">Contrôle & Gel dévastateur</p>
          <div className="path-talents">
            <span className="talent-tag">Rebonds</span>
            <span className="talent-tag">Éclat</span>
            <span className="talent-tag legendary">Zéro Absolu</span>
          </div>
        </div>
      </section>

      {/* Footer */}
      <footer className="footer">
        <div className="footer-stats">
          <span className="footer-stat">45 TALENTS</span>
          <span className="footer-separator">•</span>
          <span className="footer-stat">9 PALIERS</span>
          <span className="footer-separator">•</span>
          <span className="footer-stat">5 VOIES</span>
        </div>
        <div className="footer-brand">
          <span className="zombie-icon">🧟</span>
          <span className="brand-name">ZOMBIEZ</span>
        </div>
      </footer>
    </div>
  );
};

export default HunterShowcase;
