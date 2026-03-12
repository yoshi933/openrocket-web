import { useState } from 'react';

const SAMPLE_MOTORS = [
  { id: 'A8-3', manufacturer: 'Estes', designation: 'A8-3', totalImpulse: '2.5 Ns', avgThrust: '8 N', burnTime: '0.32 s' },
  { id: 'B6-4', manufacturer: 'Estes', designation: 'B6-4', totalImpulse: '5.0 Ns', avgThrust: '6 N', burnTime: '0.85 s' },
  { id: 'C6-5', manufacturer: 'Estes', designation: 'C6-5', totalImpulse: '10.0 Ns', avgThrust: '6 N', burnTime: '1.7 s' },
  { id: 'D12-5', manufacturer: 'Estes', designation: 'D12-5', totalImpulse: '17.0 Ns', avgThrust: '12 N', burnTime: '1.4 s' },
];

function MotorBrowser({ motors, onSelect, selected }) {
  return (
    <div className="motor-browser">
      <div className="pane-header">Motor Browser</div>
      <table className="motor-table">
        <thead>
          <tr>
            <th>Designation</th>
            <th>Manufacturer</th>
            <th>Total Impulse</th>
            <th>Avg Thrust</th>
            <th>Burn Time</th>
          </tr>
        </thead>
        <tbody>
          {motors.map((m) => (
            <tr
              key={m.id}
              className={selected?.id === m.id ? 'motor-row motor-row--selected' : 'motor-row'}
              onClick={() => onSelect(m)}
            >
              <td>{m.designation}</td>
              <td>{m.manufacturer}</td>
              <td>{m.totalImpulse}</td>
              <td>{m.avgThrust}</td>
              <td>{m.burnTime}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function MotorDetail({ motor }) {
  if (!motor) {
    return (
      <div className="motor-detail">
        <div className="pane-header">Motor Detail</div>
        <p className="placeholder-text">Select a motor to see details.</p>
      </div>
    );
  }
  return (
    <div className="motor-detail">
      <div className="pane-header">Motor Detail — {motor.designation}</div>
      <dl className="motor-detail__list">
        <dt>Manufacturer</dt><dd>{motor.manufacturer}</dd>
        <dt>Total Impulse</dt><dd>{motor.totalImpulse}</dd>
        <dt>Average Thrust</dt><dd>{motor.avgThrust}</dd>
        <dt>Burn Time</dt><dd>{motor.burnTime}</dd>
      </dl>
    </div>
  );
}

function ThrustCurveGraph({ motor }) {
  if (!motor) return null;
  return (
    <div className="thrust-curve-graph">
      <div className="pane-header">Thrust Curve — {motor.designation}</div>
      <svg width="100%" height="120" viewBox="0 0 300 100" xmlns="http://www.w3.org/2000/svg">
        <polyline
          points="0,100 10,30 40,20 80,25 150,28 250,60 290,95 300,100"
          fill="none"
          stroke="#1565c0"
          strokeWidth="2"
        />
        <text x="150" y="95" textAnchor="middle" fontSize="10" fill="#555">
          Thrust curve (placeholder)
        </text>
      </svg>
    </div>
  );
}

export default function Motors() {
  const [selected, setSelected] = useState(null);

  return (
    <div className="motors-view">
      <MotorBrowser motors={SAMPLE_MOTORS} onSelect={setSelected} selected={selected} />
      <div className="motors-side">
        <MotorDetail motor={selected} />
        <ThrustCurveGraph motor={selected} />
      </div>
    </div>
  );
}
