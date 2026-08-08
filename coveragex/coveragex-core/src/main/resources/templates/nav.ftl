<#import "navTree.ftl" as navTree>
<#import "utils.ftl" as u>

<#macro render report>
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
    <#if report.hasModules()>
      <#list report.modules as m>
        <@renderModule module=m />
      </#list>
    <#else>
      <@navTree.renderList nodes=report.navTree depth=0 />
    </#if>
  </div>
</nav>
</#macro>

<#macro renderModule module>
<section class="nav-folder nav-module${module.expandedByDefault?then(" open","")}"
         data-scope="${module.scopeId}"
         data-has-crit="${module.hasCriticalInsight?string("1","")}"
         data-has-warn="${module.hasWarningInsight?string("1","")}">
  <div class="nav-folder-row nav-module-row" style="--depth:0" onclick="toggleNavFolder(this)">
    <span class="nav-arrow">▶</span>
    <span class="nav-name">${module.displayName?html}</span>
    <span class="nav-pct ${u.pctClass(module.coveragePercent)}">${module.coveragePercent?round}%</span>
    <span class="nav-badges">
      <#if module.hasCriticalInsight><span class="nav-dot crit"></span></#if>
      <#if module.hasWarningInsight><span class="nav-dot warn"></span></#if>
    </span>
  </div>
  <div class="nav-folder-children">
    <@navTree.renderList nodes=module.navTree depth=1 />
  </div>
</section>
</#macro>
