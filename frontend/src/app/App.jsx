import { useState } from 'react';
import MenuBar from './MenuBar.jsx';
import Tabs from './Tabs.jsx';
import ComponentTree from '../ui/design/ComponentTree/index.jsx';
import RocketView from '../ui/design/RocketView/index.jsx';
import ComponentEditor from '../ui/design/ComponentEditor/index.jsx';
import Motors from '../ui/motors/index.jsx';
import Simulation from '../ui/simulation/index.jsx';

function DesignView() {
  return (
    <div className="design-panes">
      <aside className="pane pane--left">
        <ComponentTree />
      </aside>
      <main className="pane pane--center">
        <RocketView />
      </main>
      <aside className="pane pane--right">
        <ComponentEditor />
      </aside>
    </div>
  );
}

function FlightDataView() {
  return (
    <div className="placeholder-view">
      <h2>Flight Data</h2>
      <p>Flight data analysis will be displayed here.</p>
    </div>
  );
}

export default function App() {
  const [activeTab, setActiveTab] = useState('design');

  function renderTabContent() {
    switch (activeTab) {
      case 'design':
        return <DesignView />;
      case 'motors':
        return <Motors />;
      case 'simulation':
        return <Simulation />;
      case 'flightdata':
        return <FlightDataView />;
      default:
        return <DesignView />;
    }
  }

  return (
    <div className="app-shell">
      <MenuBar />
      <div className="app-content">
        {renderTabContent()}
      </div>
      <Tabs activeTab={activeTab} onTabChange={setActiveTab} />
    </div>
  );
}
