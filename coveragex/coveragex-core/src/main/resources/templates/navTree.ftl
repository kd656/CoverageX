<#import "utils.ftl" as u>

<#macro renderList nodes depth>
  <#list nodes as node>
    <#if node.isClassGroup()>
      <@renderClassGroup node=node depth=depth />
    <#elseif node.isFolder()>
      <@renderFolder node=node depth=depth />
    <#else>
      <@renderFile node=node depth=depth />
    </#if>
  </#list>
</#macro>

<#macro renderFolder node depth>
<div class="nav-folder${node.expandedByDefault?then(" open","")}"
     data-path="${node.path?html}"
     data-has-crit="${node.hasCriticalInsight?string("1","")}"
     data-has-warn="${node.hasWarningInsight?string("1","")}">
  <div class="nav-folder-row" style="--depth:${depth}" onclick="toggleNavFolder(this)">
    <span class="nav-arrow">▶</span>
    <span class="nav-name">${node.label?html}</span>
    <span class="nav-pct ${u.pctClass(node.averageCoveragePercent)}">${node.averageCoveragePercent?round}%</span>
    <span class="nav-badges">
      <#if node.hasCriticalInsight><span class="nav-dot crit"></span></#if>
      <#if node.hasWarningInsight><span class="nav-dot warn"></span></#if>
    </span>
  </div>
  <div class="nav-folder-children">
    <@renderList nodes=node.children depth=depth+1 />
  </div>
</div>
</#macro>

<#macro renderClassGroup node depth>
<div class="nav-folder nav-class-group${node.expandedByDefault?then(" open","")}"
     data-has-crit="${node.hasCriticalInsight?string("1","")}"
     data-has-warn="${node.hasWarningInsight?string("1","")}"
     data-has-pos="${node.hasPositiveInsight?string("1","")}">
  <div class="nav-folder-row nav-class-row nav-item"
       data-section="${node.sectionId}"
       data-payload="${node.payloadPath}"
       data-has-crit="${node.hasCriticalInsight?string("1","")}"
       data-has-warn="${node.hasWarningInsight?string("1","")}"
       data-has-pos="${node.hasPositiveInsight?string("1","")}"
       style="--depth:${depth}"
       onclick="loadClass('${node.sectionId}','${node.payloadPath}');return false;">
    <span class="nav-arrow" onclick="toggleNavFolder(this.parentElement);event.stopPropagation();return false;">▶</span>
    <span class="nav-name">${node.label?html}</span>
    <span class="nav-pct ${u.pctClass(node.coveragePercent)}">${node.coveragePercent?round}%</span>
    <span class="nav-badges">
      <#if node.hasCriticalInsight><span class="nav-dot crit"></span></#if>
      <#if node.hasWarningInsight><span class="nav-dot warn"></span></#if>
      <#if node.hasPositiveInsight><span class="nav-dot pos"></span></#if>
    </span>
  </div>
  <div class="nav-folder-children">
    <@renderList nodes=node.children depth=depth+1 />
  </div>
</div>
</#macro>

<#macro renderFile node depth>
<a class="nav-item" href="#"
   data-section="${node.sectionId}"
   data-payload="${node.payloadPath}"
   data-has-crit="${node.hasCriticalInsight?string("1","")}"
   data-has-warn="${node.hasWarningInsight?string("1","")}"
   data-has-pos="${node.hasPositiveInsight?string("1","")}"
   style="--depth:${depth}"
   onclick="loadClass('${node.sectionId}','${node.payloadPath}');return false;">
  <span class="nav-name">${node.simpleName?html}</span>
  <span class="nav-pct ${u.pctClass(node.coveragePercent)}">${node.coveragePercent?round}%</span>
  <span class="nav-badges">
    <#if node.hasCriticalInsight><span class="nav-dot crit"></span></#if>
    <#if node.hasWarningInsight><span class="nav-dot warn"></span></#if>
    <#if node.hasPositiveInsight><span class="nav-dot pos"></span></#if>
  </span>
</a>
</#macro>
