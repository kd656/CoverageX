<#import "utils.ftl" as u>
<#macro render model>
<#assign donutR    = 13>
<#assign donutCirc = 2 * 3.14159265 * donutR>
<#assign donutFill = donutCirc * model.coveragePct / 100>
<#assign donutGap  = donutCirc - donutFill>
<#assign strokeColor = (model.coveragePct gte 80)?string("#a6e3a1",
                       (model.coveragePct gte 50)?string("#e5c07b", "#f38ba8"))>
<header id="topbar">
  <button id="nav-toggle" title="Toggle sidebar">☰</button>
  <div class="brand">Coverage<span>X</span></div>
  <div class="divider"></div>
  <div class="meta">${model.timestamp}</div>
  <div class="coverage-pill">
    <div class="donut-wrap">
      <svg width="36" height="36" viewBox="0 0 36 36">
        <circle cx="18" cy="18" r="13" fill="none" stroke="#313248" stroke-width="5"/>
        <circle cx="18" cy="18" r="13" fill="none" stroke="${strokeColor}" stroke-width="5"
                stroke-dasharray="${donutFill?c} ${donutGap?c}" stroke-linecap="round"/>
      </svg>
    </div>
    <div class="coverage-label ${u.pctClass(model.coveragePct)}">${model.coveragePct?string["0.0"]}%</div>
  </div>
  <div class="divider"></div>
  <div class="stat-group">
    <div class="stat"><span class="val">${model.classCount}</span><span class="lbl">classes</span></div>
    <div class="stat crit"><span class="val">${model.critCount}</span><span class="lbl">critical</span></div>
    <div class="stat warn"><span class="val">${model.warnCount}</span><span class="lbl">warnings</span></div>
    <div class="stat info"><span class="val">${model.infoCount}</span><span class="lbl">info</span></div>
    <div class="stat pos"><span class="val">${model.posCount}</span><span class="lbl">optimal</span></div>
  </div>
  <div class="divider"></div>
  <div class="actions">
    <button class="btn" id="btn-jump">↓ Next uncovered</button>
  </div>
</header>
</#macro>
