<#-- utils.ftl: presentation helpers. Import with <#import "utils.ftl" as u> -->

<#-- Coverage percentage → CSS class name -->
<#function pctClass pct>
  <#if pct gte 80><#return "high">
  <#elseif pct gte 50><#return "mid">
  <#elseif pct gt 0><#return "low">
  <#else><#return "zero">
  </#if>
</#function>

<#-- Fill bar class (for the coverage bar fill element) -->
<#function fillClass pct>
  <#if pct gte 80><#return "fill-high">
  <#elseif pct gte 50><#return "fill-mid">
  <#elseif pct gt 0><#return "fill-low">
  <#else><#return "fill-zero">
  </#if>
</#function>

<#-- Clamped fill % (min 2 so zero-coverage bar is still visible) -->
<#function fillPct pct>
  <#return [2, [100, pct?round]?min]?max>
</#function>

<#-- Severity enum name → CSS class -->
<#function severityCss sev>
  <#switch sev>
    <#case "CRITICAL"><#return "crit">
    <#case "WARNING"><#return "warn">
    <#case "INFO"><#return "info">
    <#default><#return "pos">
  </#switch>
</#function>

<#-- Severity enum name → icon character -->
<#function severityIcon sev>
  <#switch sev>
    <#case "CRITICAL"><#return "✕">
    <#case "WARNING"><#return "!">
    <#case "INFO"><#return "i">
    <#default><#return "✓">
  </#switch>
</#function>

<#-- ConditionCoverage enum → branch badge CSS class -->
<#function conditionCoverageCss cc>
  <#switch cc>
    <#case "ALL"><#return "covered">
    <#case "PARTIAL"><#return "semi-covered">
    <#default><#return "not-covered">
  </#switch>
</#function>

<#-- Coverage enum → source row CSS class -->
<#function rowClass coverage hasProbes>
  <#if !hasProbes><#return "line"></#if>
  <#switch coverage>
    <#case "HIT"><#return "line hit">
    <#case "MISS"><#return "line miss">
    <#case "PARTIAL_BRANCH"><#return "line branch-partial">
    <#default><#return "line">
  </#switch>
</#function>
