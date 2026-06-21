<#import "navTree.ftl" as navTree>
<#macro render tree>
<nav id="nav">
  <div id="nav-resizer"></div>
  <div id="nav-filter-wrap">
    <input id="nav-filter" type="text" placeholder="Filter classes…" oninput="filterNav(this.value)">
    <div class="filter-row">
      <button class="filter-chip" onclick="filterSeverity('crit',this)">✗ critical</button>
      <button class="filter-chip" onclick="filterSeverity('warn',this)">⚠ warnings</button>
      <button class="filter-chip" onclick="filterSeverity('pos',this)">✓ optimal</button>
    </div>
  </div>
  <div id="nav-list">
    <@navTree.renderList nodes=tree depth=0 />
  </div>
</nav>
</#macro>
