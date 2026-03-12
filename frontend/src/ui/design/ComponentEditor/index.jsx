const SAMPLE_FIELDS = [
  { label: 'Name', type: 'text', value: 'Nose Cone' },
  { label: 'Length (mm)', type: 'number', value: '200' },
  { label: 'Base Diameter (mm)', type: 'number', value: '50' },
  { label: 'Wall Thickness (mm)', type: 'number', value: '2' },
  { label: 'Material', type: 'select', value: 'Balsa', options: ['Balsa', 'Fiberglass', 'Carbon Fiber', 'Plywood'] },
  { label: 'Finish', type: 'select', value: 'Smooth', options: ['Smooth', 'Rough', 'Painted'] },
];

function Field({ field }) {
  if (field.type === 'select') {
    return (
      <div className="editor-field">
        <label className="editor-field__label">{field.label}</label>
        <select className="editor-field__input" defaultValue={field.value}>
          {field.options.map((opt) => (
            <option key={opt} value={opt}>{opt}</option>
          ))}
        </select>
      </div>
    );
  }
  return (
    <div className="editor-field">
      <label className="editor-field__label">{field.label}</label>
      <input className="editor-field__input" type={field.type} defaultValue={field.value} />
    </div>
  );
}

export default function ComponentEditor() {
  return (
    <div className="component-editor">
      <div className="pane-header">Component Editor</div>
      <div className="editor-fields">
        {SAMPLE_FIELDS.map((field) => (
          <Field key={field.label} field={field} />
        ))}
      </div>
    </div>
  );
}
