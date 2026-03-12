const TABS = [
  { id: 'design', label: 'Design' },
  { id: 'motors', label: 'Motors' },
  { id: 'simulation', label: 'Simulation' },
  { id: 'flightdata', label: 'Flight Data' },
];

export default function Tabs({ activeTab, onTabChange }) {
  return (
    <nav className="bottom-tabs">
      {TABS.map((tab) => (
        <button
          key={tab.id}
          className={`bottom-tab ${activeTab === tab.id ? 'bottom-tab--active' : ''}`}
          onClick={() => onTabChange(tab.id)}
        >
          {tab.label}
        </button>
      ))}
    </nav>
  );
}
