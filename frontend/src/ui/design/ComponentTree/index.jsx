const SAMPLE_TREE = [
  {
    id: 'rocket',
    label: 'Rocket',
    children: [
      {
        id: 'sustainer',
        label: 'Sustainer',
        children: [
          { id: 'nosecone', label: 'Nose Cone', children: [] },
          { id: 'bodytube', label: 'Body Tube', children: [
            { id: 'fins', label: 'Fin Set', children: [] },
            { id: 'launchlug', label: 'Launch Lug', children: [] },
          ]},
          { id: 'motormount', label: 'Motor Mount', children: [
            { id: 'motor', label: 'Motor', children: [] },
          ]},
        ],
      },
    ],
  },
];

function TreeNode({ node, depth = 0 }) {
  return (
    <li className="tree-node" style={{ paddingLeft: `${depth * 16}px` }}>
      <span className="tree-node__label">{node.label}</span>
      {node.children && node.children.length > 0 && (
        <ul className="tree-node__children">
          {node.children.map((child) => (
            <TreeNode key={child.id} node={child} depth={depth + 1} />
          ))}
        </ul>
      )}
    </li>
  );
}

export default function ComponentTree() {
  return (
    <div className="component-tree">
      <div className="pane-header">Component Tree</div>
      <ul className="tree-root">
        {SAMPLE_TREE.map((node) => (
          <TreeNode key={node.id} node={node} />
        ))}
      </ul>
    </div>
  );
}
