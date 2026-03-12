import { useState } from 'react';

const MENUS = [
  {
    label: 'File',
    items: ['New', 'Open...', 'Save', 'Save As...', '---', 'Import .ork...', 'Export .ork...', '---', 'Exit'],
  },
  {
    label: 'Edit',
    items: ['Undo', 'Redo', '---', 'Cut', 'Copy', 'Paste', 'Delete'],
  },
  {
    label: 'Tools',
    items: ['Preferences', 'Component Analysis', 'Rocket Optimization'],
  },
  {
    label: 'Help',
    items: ['Documentation', 'About OpenRocket Web'],
  },
];

export default function MenuBar() {
  const [openMenu, setOpenMenu] = useState(null);

  function toggleMenu(label) {
    setOpenMenu(openMenu === label ? null : label);
  }

  function closeAll() {
    setOpenMenu(null);
  }

  return (
    <nav className="menu-bar" onMouseLeave={closeAll}>
      {MENUS.map((menu) => (
        <div key={menu.label} className="menu-item">
          <button
            className={`menu-trigger ${openMenu === menu.label ? 'menu-trigger--active' : ''}`}
            onClick={() => toggleMenu(menu.label)}
          >
            {menu.label}
          </button>
          {openMenu === menu.label && (
            <ul className="menu-dropdown">
              {menu.items.map((item, idx) =>
                item === '---' ? (
                  <li key={idx} className="menu-separator" role="separator" />
                ) : (
                  <li key={item}>
                    <button className="menu-dropdown-item" onClick={closeAll}>
                      {item}
                    </button>
                  </li>
                )
              )}
            </ul>
          )}
        </div>
      ))}
    </nav>
  );
}
