<#macro render>
<div id="invocation-popover" class="popover" style="display:none">
  <div class="popover-arrow"></div>
  <div class="popover-header">
    <span class="ph-title" id="inv-title">method</span>
    <span class="ph-subtitle" id="inv-subtitle"></span>
    <button class="ph-close" onclick="closePopovers()">✕</button>
  </div>
  <div class="inv-summary">
    <div class="inv-stat"><span class="is-val" id="inv-unique-tests">0</span><span class="is-lbl">unique tests</span></div>
    <div class="inv-stat"><span class="is-val" id="inv-unique-args">0</span><span class="is-lbl">arg combos</span></div>
  </div>
  <div class="inv-footer">
    <span class="dot"></span>
    <span id="inv-footer-note">Test tracking requires the CoverageX test listener agent.</span>
  </div>
  <div class="inv-table-wrap">
    <table><thead><tr id="inv-thead-row">
      <th>Test method</th><th style="text-align:right">Calls</th>
    </tr></thead><tbody id="inv-tbody"></tbody></table>
  </div>
</div>

<div id="branch-popover" class="popover" style="display:none">
  <div class="popover-arrow"></div>
  <div class="popover-header">
    <span class="ph-title">Branch coverage</span>
    <span class="ph-subtitle" id="bp-line-label"></span>
    <button class="ph-close" onclick="closePopovers()">✕</button>
  </div>
  <div id="bp-condition-selector" class="bp-condition" style="display:none">
    <div class="bc-label">Condition</div>
    <select id="bp-condition-select" onchange="onBranchConditionChange()"></select>
  </div>
  <div id="bp-condition-single" class="bp-condition">
    <div class="bc-label">Condition</div>
    <code id="bp-condition-text"></code>
  </div>
  <div class="bp-direction">
    <div class="bp-dir" id="bp-true-dir">
      <div class="dir-label">TRUE branch</div>
      <div class="dir-status" id="bp-true-status">—</div>
      <div class="dir-count" id="bp-true-count"></div>
    </div>
    <div style="width:8px"></div>
    <div class="bp-dir" id="bp-false-dir">
      <div class="dir-label">FALSE branch</div>
      <div class="dir-status" id="bp-false-status">—</div>
      <div class="dir-count" id="bp-false-count"></div>
    </div>
  </div>
  <div class="bp-hint" id="bp-hint-row">
    <span class="bh-icon" id="bp-hint-icon">💡</span>
    <span class="bh-text" id="bp-hint-text"></span>
  </div>
  <div class="bp-summary">
    <div class="bp-stat"><span class="bs-val" id="bp-unique-tests">0</span><span class="bs-lbl">unique tests</span></div>
    <div class="bp-stat"><span class="bs-val" id="bp-directions-covered">0/2</span><span class="bs-lbl">directions covered</span></div>
  </div>
  <div class="bp-footer">
    <span class="dot"></span>
    <span id="bp-footer-note">Test tracking requires the CoverageX test listener.</span>
  </div>
  <div class="bp-table-wrap">
    <table><thead><tr id="bp-tests-thead-row">
      <th>Test method</th>
      <th style="text-align:right">Calls</th>
      <th style="text-align:center">Branch</th>
    </tr></thead><tbody id="bp-tests-tbody"></tbody></table>
  </div>
</div>
</#macro>
