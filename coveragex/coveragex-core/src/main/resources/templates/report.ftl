<#import "topbar.ftl" as topbar>
<#import "nav.ftl" as nav>
<#import "popovers.ftl" as popovers>
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>CoverageX Report</title>
  <style>
    *, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; }
    :root {
      --bg: #1e1e2e; --bg-raised: #242436; --bg-hover: #2a2a3e;
      --border: #313248; --text: #cdd6f4; --text-muted: #6c7086; --text-dim: #45475a;
      --badge-crit: #e74c3c; --badge-warn: #e5c07b; --badge-info: #89b4fa; --badge-pos: #2ecc71;
      --radius: 6px; --mono: 'JetBrains Mono','Fira Code','Cascadia Code',monospace;
    }
    body {
      font-family: -apple-system,BlinkMacSystemFont,'Segoe UI',sans-serif;
      background: var(--bg); color: var(--text); font-size: 14px; line-height: 1.5;
      display: grid; grid-template-rows: 56px 1fr; grid-template-columns: var(--nav-w, 260px) 1fr;
      grid-template-areas: "topbar topbar" "nav main"; min-height: 100vh;
      transition: var(--nav-transition, none);
    }
    #topbar {
      grid-area: topbar; background: var(--bg-raised); border-bottom: 1px solid var(--border);
      display: flex; align-items: center; gap: 24px; padding: 0 24px;
      position: sticky; top: 0; z-index: 100;
    }
    #topbar .brand { font-weight: 700; font-size: 15px; letter-spacing: .5px; }
    #topbar .brand span { color: #00fb98; }
    #topbar .meta { color: var(--text-muted); font-size: 12px; }
    .divider { width: 1px; height: 20px; background: var(--border); }
    .coverage-pill { display: flex; align-items: center; gap: 10px; }
    .donut-wrap { position: relative; width: 36px; height: 36px; }
    .donut-wrap svg { transform: rotate(-90deg); }
    .donut-wrap .pct {
      position: absolute; inset: 0; display: flex;
      align-items: center; justify-content: center; font-size: 9px; font-weight: 700;
    }
    .coverage-label { font-size: 22px; font-weight: 700; }
    .coverage-label.high { color: #a6e3a1; }
    .coverage-label.mid { color: #f9e2af; }
    .coverage-label.low { color: #f38ba8; }
    .stat-group { display: flex; gap: 16px; margin-left: auto; }
    .stat { display: flex; flex-direction: column; align-items: center; gap: 2px; }
    .stat .val { font-size: 16px; font-weight: 700; }
    .stat .lbl { font-size: 10px; color: var(--text-muted); text-transform: uppercase; letter-spacing: .5px; }
    .stat.crit .val { color: var(--badge-crit); }
    .stat.warn .val { color: var(--badge-warn); }
    .stat.info .val { color: var(--badge-info); }
    .stat.pos .val { color: var(--badge-pos); }
    #topbar .actions { display: flex; gap: 8px; }
    .btn {
      padding: 5px 12px; border-radius: var(--radius); border: 1px solid var(--border);
      background: transparent; color: var(--text); font-size: 12px; cursor: pointer;
      transition: background .15s; white-space: nowrap;
    }
    .btn:hover { background: var(--bg-hover); }
    .btn.primary { background: #313248; border-color: #89b4fa; color: #89b4fa; }
    /* NAV */
    #nav-toggle {
      background: none; border: none; color: var(--text-muted); font-size: 16px;
      cursor: pointer; padding: 4px 8px; border-radius: var(--radius);
      transition: background .15s, color .15s; line-height: 1; flex-shrink: 0;
    }
    #nav-toggle:hover { background: var(--bg-hover); color: var(--text); }
    #nav {
      grid-area: nav; background: var(--bg-raised); border-right: 1px solid var(--border);
      display: flex; flex-direction: column; overflow: hidden;
      position: sticky; top: 56px; height: calc(100vh - 56px);
      min-width: 0;
    }
    #nav-resizer {
      position: absolute; right: 0; top: 0; bottom: 0; width: 5px;
      cursor: col-resize; z-index: 10; background: transparent;
      transition: background .15s;
    }
    #nav-resizer:hover, #nav-resizer.dragging { background: rgba(137,180,250,.45); }
    body.nav-resizing { cursor: col-resize !important; user-select: none !important; }
    #nav-filter-wrap { padding: 12px; border-bottom: 1px solid var(--border); }
    #nav-filter {
      width: 100%; padding: 6px 10px; background: var(--bg); border: 1px solid var(--border);
      border-radius: var(--radius); color: var(--text); font-size: 13px; outline: none;
    }
    #nav-filter:focus { border-color: #89b4fa; }
    #nav-filter::placeholder { color: var(--text-dim); }
    .filter-row { display: flex; gap: 4px; margin-top: 8px; }
    .filter-chip {
      flex: 1; padding: 3px 0; border-radius: 4px; border: 1px solid var(--border);
      background: transparent; color: var(--text-muted); font-size: 10px;
      cursor: pointer; text-align: center; transition: all .15s;
    }
    .filter-chip:hover { background: var(--bg-hover); color: var(--text); }
    .filter-chip.active-crit { background: rgba(231,76,60,.15); border-color: var(--badge-crit); color: var(--badge-crit); }
    .filter-chip.active-warn { background: rgba(229,192,123,.15); border-color: var(--badge-warn); color: var(--badge-warn); }
    .filter-chip.active-pos  { background: rgba(46,204,113,.15); border-color: var(--badge-pos); color: var(--badge-pos); }
    #nav-list { overflow-y: auto; flex: 1; padding: 8px 0; }
    #nav-list::-webkit-scrollbar { width: 4px; }
    #nav-list::-webkit-scrollbar-thumb { background: var(--border); border-radius: 4px; }
    /* Tree node styles */
    .nav-folder-row {
      display: flex; align-items: center; gap: 6px;
      padding: 5px 0; padding-left: calc(12px + var(--depth) * 14px);
      cursor: pointer; color: var(--text-muted);
      font-size: 12px; text-transform: uppercase; letter-spacing: .4px;
      transition: background .1s; user-select: none;
    }
    .nav-folder-row:hover { background: var(--bg-hover); color: var(--text); }
    .nav-class-row { text-transform: none; letter-spacing: 0; }
    /* Nested classes: mark direct children of a class-group with a ↳ prefix
       so the reader can see the class-nesting relationship without relying
       on indentation alone. */
    .nav-class-group > .nav-folder-children > .nav-item > .nav-name::before,
    .nav-class-group > .nav-folder-children > .nav-folder > .nav-folder-row > .nav-name::before {
      content: "↳ ";
      color: var(--text-muted);
      margin-right: 2px;
    }
    .nav-arrow { font-size: 8px; transition: transform .15s; min-width: 10px; }
    .nav-folder.open > .nav-folder-row .nav-arrow { transform: rotate(90deg); }
    .nav-folder-children { display: none; }
    .nav-folder.open > .nav-folder-children { display: block; }
    /* Module-level nav rows (scoped reports) */
    .nav-module-row {
      font-weight: 600; letter-spacing: .5px;
      background: rgba(255,255,255,.02);
      border-top: 1px solid var(--border);
    }
    .nav-module:first-of-type .nav-module-row { border-top: none; }
    .nav-module-row .nav-name { color: var(--text); }
    .nav-item {
      display: flex; align-items: center; gap: 8px;
      padding: 7px 0; padding-left: calc(12px + var(--depth) * 14px);
      cursor: pointer; text-decoration: none; color: var(--text); transition: background .1s;
      border-left: 3px solid transparent;
    }
    .nav-item:hover { background: var(--bg-hover); }
    .nav-item.active { background: var(--bg-hover); border-left-color: #89b4fa; }
    .nav-item .nav-name {
      flex: 1; font-size: 13px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
    }
    .nav-pct { font-size: 11px; font-weight: 600; min-width: 36px; text-align: right; }
    .nav-pct.high { color: #a6e3a1; }
    .nav-pct.mid { color: #f9e2af; }
    .nav-pct.low { color: #f38ba8; }
    .nav-pct.zero { color: var(--badge-crit); }
    .nav-badges { display: flex; gap: 3px; padding-right: 8px; }
    .nav-dot { width: 6px; height: 6px; border-radius: 50%; }
    .nav-dot.crit { background: var(--badge-crit); }
    .nav-dot.warn { background: var(--badge-warn); }
    .nav-dot.pos { background: var(--badge-pos); }
    /* MAIN area */
    #main {
      grid-area: main; overflow-y: auto; padding: 24px;
      display: flex; flex-direction: column; gap: 20px;
    }
    #main::-webkit-scrollbar { width: 6px; }
    #main::-webkit-scrollbar-thumb { background: var(--border); border-radius: 6px; }
    /* Empty / loading / error states */
    .empty-state {
      display: flex; flex-direction: column; align-items: center; justify-content: center;
      height: 60vh; gap: 12px; color: var(--text-muted); text-align: center;
    }
    .empty-state h2 { font-size: 20px; font-weight: 600; color: var(--text); }
    .empty-state p { font-size: 14px; }
    .loading-spinner {
      display: flex; align-items: center; justify-content: center;
      height: 60vh; color: var(--text-muted); font-size: 14px;
    }
    .load-error {
      display: flex; align-items: center; justify-content: center;
      height: 60vh; color: var(--badge-crit); font-size: 14px;
    }
    /* Class section */
    .class-section {
      background: var(--bg-raised); border: 1px solid var(--border);
      border-radius: var(--radius); overflow: hidden;
    }
    .class-header {
      display: flex; align-items: center; gap: 10px;
      padding: 12px 16px; user-select: none;
      border-bottom: 1px solid var(--border);
    }
    .class-header .class-name { font-size: 14px; font-weight: 600; flex: 1; }
    .class-header .package-name { font-size: 11px; color: var(--text-muted); font-weight: 400; }
    .coverage-bar-wrap { display: flex; align-items: center; gap: 8px; }
    .coverage-bar { width: 80px; height: 6px; background: var(--bg); border-radius: 3px; overflow: hidden; }
    .coverage-bar-fill { height: 100%; border-radius: 3px; }
    .fill-high { background: #2ecc71; }
    .fill-mid { background: #e5c07b; }
    .fill-low { background: #e74c3c; }
    .fill-zero { background: #5a1e1e; }
    .coverage-bar-pct { font-size: 12px; font-weight: 600; min-width: 38px; text-align: right; }
    .coverage-bar-pct.high { color: #a6e3a1; }
    .coverage-bar-pct.mid { color: #f9e2af; }
    .coverage-bar-pct.low { color: #f38ba8; }
    .coverage-bar-pct.zero { color: var(--badge-crit); }
    .badge {
      display: inline-flex; align-items: center; gap: 4px;
      padding: 2px 7px; border-radius: 10px; font-size: 11px;
      font-weight: 600; white-space: nowrap;
    }
    .badge.crit { background: rgba(231,76,60,.15); color: var(--badge-crit); border: 1px solid rgba(231,76,60,.3); }
    .badge.warn { background: rgba(229,192,123,.15); color: var(--badge-warn); border: 1px solid rgba(229,192,123,.3); }
    .badge.info { background: rgba(137,180,250,.15); color: var(--badge-info); border: 1px solid rgba(137,180,250,.3); }
    .badge.pos  { background: rgba(46,204,113,.15); color: var(--badge-pos); border: 1px solid rgba(46,204,113,.3); }
    /* Insights panel */
    .insights-panel {
      padding: 10px 16px; border-bottom: 1px solid var(--border);
      display: flex; flex-direction: column; gap: 6px; background: rgba(0,0,0,.15);
    }
    .insight {
      display: flex; align-items: flex-start; gap: 10px;
      padding: 7px 10px; border-radius: var(--radius); font-size: 12px; line-height: 1.5;
    }
    .insight.C { background: rgba(231,76,60,.08); border-left: 3px solid var(--badge-crit); }
    .insight.W { background: rgba(229,192,123,.08); border-left: 3px solid var(--badge-warn); }
    .insight.I { background: rgba(137,180,250,.08); border-left: 3px solid var(--badge-info); }
    .insight.P { background: rgba(46,204,113,.08); border-left: 3px solid var(--badge-pos); }
    .insight .icon { font-size: 13px; margin-top: 1px; min-width: 16px; }
    .insight .body { flex: 1; }
    .insight .body .title { font-weight: 600; }
    .insight .body .detail { color: var(--text-muted); margin-top: 2px; }
    .insight .body code {
      font-family: var(--mono); font-size: 11px;
      background: rgba(255,255,255,.07); padding: 0 4px; border-radius: 3px;
    }
    .insight .line-ref {
      font-size: 11px; color: var(--text-dim); white-space: nowrap;
      cursor: pointer; padding: 2px 6px; border-radius: 4px; margin-left: auto;
    }
    .insight .line-ref:hover { background: var(--bg-hover); color: var(--text); }
    /* Source table */
    .source-view { width: 100%; border-collapse: collapse; font-family: var(--mono); font-size: 13px; }
    .source-view tr { transition: background .08s; }
    .source-view tr:hover { background: rgba(255,255,255,.03); }
    .line td:first-child { border-left: 3px solid transparent; }
    .line.hit { background: rgba(46,204,113,.06); }
    .line.hit td:first-child { border-left-color: rgba(46,204,113,.4); }
    .line.miss { background: rgba(231,76,60,.07); }
    .line.miss td:first-child { border-left-color: rgba(231,76,60,.4); }
    .line.branch-partial { background: rgba(229,192,123,.07); }
    .line.branch-partial td:first-child { border-left-color: rgba(229,192,123,.4); }
    td.ln {
      width: 48px; text-align: right; padding: 2px 10px 2px 4px;
      color: var(--text-dim); font-size: 12px; user-select: none; vertical-align: top;
    }
    td.hits {
      width: 44px; text-align: right; padding: 2px 8px;
      font-size: 11px; font-weight: 600; color: var(--text-dim);
      user-select: none; vertical-align: top; white-space: nowrap;
    }
    .line.hit td.hits { color: rgba(166,227,161,.55); }
    .line.miss td.hits { color: rgba(243,139,168,.45); }
    .line.branch-partial td.hits { color: rgba(249,226,175,.5); }
    td.src { padding: 2px 12px 2px 8px; }
    td.src code { font-family: inherit; font-size: inherit; white-space: pre; }
    tr.method-separator td { padding: 0; border-top: 1px dashed var(--border); }
    /* Branch badges */
    .branch {
      display: inline-flex; align-items: center; justify-content: center;
      width: 16px; height: 16px; border-radius: 3px; font-size: 10px;
      font-weight: 700; margin-left: 5px; vertical-align: middle;
      font-family: var(--mono); position: relative; cursor: pointer;
    }
    .branch.covered      { background: rgba(46,204,113,.25); color: #a6e3a1; border: 1px solid rgba(46,204,113,.4); }
    .branch.semi-covered { background: rgba(249,226,175,.2); color: #f9e2af; border: 1px solid rgba(249,226,175,.45); }
    .branch.not-covered  { background: rgba(231,76,60,.25); color: #f38ba8; border: 1px solid rgba(231,76,60,.4); }
    /* Method marker */
    .method-marker {
      display: inline-flex; align-items: center; gap: 5px;
      font-size: 10px; font-family: -apple-system,sans-serif; color: #89b4fa;
      background: rgba(137,180,250,.1); border: 1px solid rgba(137,180,250,.3);
      border-radius: 4px; padding: 1px 7px; margin-left: 10px; vertical-align: middle;
      cursor: pointer; transition: all .15s; user-select: none;
    }
    .method-marker:hover { background: rgba(137,180,250,.2); border-color: rgba(137,180,250,.6); }
    .dead-label {
      display: inline-flex; align-items: center; gap: 4px;
      font-size: 10px; font-family: -apple-system,sans-serif; color: var(--badge-crit);
      background: rgba(231,76,60,.12); border: 1px solid rgba(231,76,60,.3);
      border-radius: 4px; padding: 1px 6px; margin-left: 8px; vertical-align: middle;
    }
    /* Method fallback */
    .no-source { padding: 16px; font-size: 12px; color: var(--text-muted); }
    .method-list { padding: 8px 16px; }
    .method-row {
      display: flex; gap: 12px; align-items: baseline;
      padding: 4px 0; border-bottom: 1px solid rgba(255,255,255,.03); font-size: 13px;
    }
    .method-row .mr-name { font-family: var(--mono); flex: 1; }
    .method-row .mr-hit { color: #a6e3a1; }
    .method-row .mr-miss { color: #f38ba8; }
    /* Popover CSS */
    .popover {
      position: fixed; z-index: 999; background: #1a1a2e; border: 1px solid var(--border);
      border-radius: 8px; box-shadow: 0 8px 32px rgba(0,0,0,.55),0 2px 8px rgba(0,0,0,.3);
      min-width: 320px; max-width: 480px;
      font-family: -apple-system,BlinkMacSystemFont,'Segoe UI',sans-serif;
      font-size: 13px; animation: popIn .12s ease-out; overflow: hidden;
    }
    @keyframes popIn {
      from { opacity: 0; transform: translateY(-4px) scale(.97); }
      to { opacity: 1; transform: translateY(0) scale(1); }
    }
    .popover-arrow { position: absolute; top: -6px; left: 20px; width: 12px; height: 6px; overflow: visible; }
    .popover-arrow::before,
    .popover-arrow::after {
      content: ''; position: absolute; left: 0;
      border-left: 6px solid transparent; border-right: 6px solid transparent;
    }
    .popover-arrow::before { top: -1px; border-bottom: 7px solid var(--border); }
    .popover-arrow::after  { top: 0;    border-bottom: 7px solid #1a1a2e; }
    .popover-header {
      display: flex; align-items: center; gap: 8px;
      padding: 10px 14px; border-bottom: 1px solid var(--border);
      background: rgba(255,255,255,.03);
    }
    .popover-header .ph-title { font-weight: 600; font-size: 13px; flex: 1; }
    .popover-header .ph-subtitle { font-size: 11px; color: var(--text-muted); }
    .popover-header .ph-close {
      width: 20px; height: 20px; border-radius: 4px; border: none;
      background: transparent; color: var(--text-muted); cursor: pointer;
      font-size: 14px; display: flex; align-items: center; justify-content: center;
      transition: all .1s;
    }
    .popover-header .ph-close:hover { background: var(--bg-hover); color: var(--text); }
    #invocation-popover .inv-summary {
      display: flex; gap: 16px; padding: 10px 14px;
      border-bottom: 1px solid var(--border); background: rgba(0,0,0,.2);
    }
    #invocation-popover .inv-stat { display: flex; flex-direction: column; gap: 2px; }
    #invocation-popover .inv-stat .is-val { font-size: 18px; font-weight: 700; color: #89b4fa; }
    #invocation-popover .inv-stat .is-lbl {
      font-size: 10px; color: var(--text-muted); text-transform: uppercase; letter-spacing: .4px;
    }
    #invocation-popover .inv-table-wrap { max-height: 260px; overflow-y: auto; }
    #invocation-popover .inv-table-wrap::-webkit-scrollbar { width: 4px; }
    #invocation-popover .inv-table-wrap::-webkit-scrollbar-thumb { background: var(--border); border-radius: 4px; }
    #invocation-popover table { width: 100%; border-collapse: collapse; font-size: 12px; }
    #invocation-popover thead tr { background: rgba(255,255,255,.03); border-bottom: 1px solid var(--border); }
    #invocation-popover th {
      padding: 6px 14px; text-align: left; font-size: 10px;
      font-weight: 600; text-transform: uppercase; letter-spacing: .5px; color: var(--text-muted);
    }
    #invocation-popover tbody tr { border-bottom: 1px solid rgba(255,255,255,.04); transition: background .08s; }
    #invocation-popover tbody tr:last-child { border-bottom: none; }
    #invocation-popover tbody tr:hover { background: var(--bg-hover); }
    #invocation-popover td { padding: 7px 14px; vertical-align: top; }
    #invocation-popover td.test-name { font-family: var(--mono); font-size: 11px; color: var(--text); }
    #invocation-popover td.inv-count {
      text-align: right; font-weight: 700; white-space: nowrap;
      color: #89b4fa; font-size: 12px; min-width: 36px;
    }
    #invocation-popover td.inv-args {
      font-family: var(--mono); font-size: 11px; color: var(--text-muted);
      max-width: 160px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
    }
    #invocation-popover td.inv-args .arg-tag {
      display: inline-block; background: rgba(255,255,255,.06);
      border: 1px solid var(--border); border-radius: 3px;
      padding: 0 4px; margin: 1px 2px 1px 0; font-size: 10px;
      max-width: 140px; overflow: hidden; text-overflow: ellipsis;
      white-space: nowrap; vertical-align: middle;
    }
    #invocation-popover td.inv-param-cell {
      font-family: var(--mono); font-size: 11px; color: var(--text-muted);
      max-width: 140px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
    }
    #invocation-popover td.inv-param-cell .arg-tag {
      display: inline-block; background: rgba(255,255,255,.06);
      border: 1px solid var(--border); border-radius: 3px;
      padding: 0 4px; margin: 1px 2px 1px 0; font-size: 10px;
      max-width: 120px; overflow: hidden; text-overflow: ellipsis;
      white-space: nowrap; vertical-align: middle;
    }
    #invocation-popover .inv-footer {
      padding: 7px 14px; border-top: 1px solid var(--border); font-size: 11px;
      color: var(--text-muted); background: rgba(0,0,0,.15);
      display: flex; align-items: center; gap: 6px;
    }
    #invocation-popover .inv-footer .dot {
      width: 6px; height: 6px; border-radius: 50%; background: #89b4fa; flex-shrink: 0;
    }
    #branch-popover .bp-direction { display: flex; gap: 1px; padding: 10px 14px 8px; }
    #branch-popover .bp-dir { flex: 1; padding: 8px 12px; border-radius: var(--radius); text-align: center; }
    #branch-popover .bp-dir.hit  { background: rgba(46,204,113,.1); border: 1px solid rgba(46,204,113,.25); }
    #branch-popover .bp-dir.miss { background: rgba(231,76,60,.1); border: 1px solid rgba(231,76,60,.25); }
    #branch-popover .bp-dir .dir-label {
      font-size: 10px; text-transform: uppercase; letter-spacing: .5px;
      color: var(--text-muted); margin-bottom: 4px;
    }
    #branch-popover .bp-dir .dir-status { font-size: 20px; font-weight: 800; }
    .bp-dir.hit .dir-status { color: #a6e3a1; }
    .bp-dir.miss .dir-status { color: #f38ba8; }
    #branch-popover .bp-dir .dir-count { font-size: 11px; color: var(--text-muted); margin-top: 2px; }
    #branch-popover .bp-condition {
      padding: 8px 14px; border-top: 1px solid var(--border);
      border-bottom: 1px solid var(--border); background: rgba(0,0,0,.2);
    }
    #branch-popover .bp-condition .bc-label {
      font-size: 10px; text-transform: uppercase; letter-spacing: .5px;
      color: var(--text-muted); margin-bottom: 4px;
    }
    #branch-popover .bp-condition code {
      font-family: var(--mono); font-size: 12px; color: var(--text);
      background: rgba(255,255,255,.05); padding: 4px 8px; border-radius: 4px; display: block;
    }
    #branch-popover #bp-condition-select {
      width: 100%; background: rgba(255,255,255,.05); border: 1px solid var(--border);
      border-radius: 4px; color: var(--text); font-family: var(--mono); font-size: 12px;
      padding: 4px 8px; outline: none; cursor: pointer;
    }
    #branch-popover #bp-condition-select:focus { border-color: #89b4fa; }
    #branch-popover .bp-hint { padding: 8px 14px; font-size: 12px; display: flex; gap: 8px; align-items: flex-start; }
    #branch-popover .bp-hint .bh-icon { font-size: 13px; margin-top: 1px; }
    #branch-popover .bp-hint .bh-text { color: var(--text-muted); line-height: 1.5; }
    #branch-popover .bp-hint .bh-text strong { color: var(--text); }
    #branch-popover .bp-summary {
      display: flex; gap: 16px; padding: 10px 14px;
      border-top: 1px solid var(--border); border-bottom: 1px solid var(--border);
      background: rgba(0,0,0,.2);
    }
    #branch-popover .bp-stat { display: flex; flex-direction: column; gap: 2px; }
    #branch-popover .bp-stat .bs-val { font-size: 18px; font-weight: 700; color: #89b4fa; }
    #branch-popover .bp-stat .bs-lbl {
      font-size: 10px; color: var(--text-muted); text-transform: uppercase; letter-spacing: .4px;
    }
    #branch-popover .bp-footer {
      padding: 7px 14px; border-bottom: 1px solid var(--border); font-size: 11px;
      color: var(--text-muted); background: rgba(0,0,0,.15);
      display: flex; align-items: center; gap: 6px;
    }
    #branch-popover .bp-footer .dot {
      width: 6px; height: 6px; border-radius: 50%; background: #89b4fa; flex-shrink: 0;
    }
    #branch-popover .bp-table-wrap { max-height: 260px; overflow-y: auto; }
    #branch-popover .bp-table-wrap::-webkit-scrollbar { width: 4px; }
    #branch-popover .bp-table-wrap::-webkit-scrollbar-thumb { background: var(--border); border-radius: 4px; }
    #branch-popover table { width: 100%; border-collapse: collapse; font-size: 12px; }
    #branch-popover thead tr { background: rgba(255,255,255,.03); border-bottom: 1px solid var(--border); }
    #branch-popover th {
      padding: 6px 14px; text-align: left; font-size: 10px;
      font-weight: 600; text-transform: uppercase; letter-spacing: .5px; color: var(--text-muted);
    }
    #branch-popover tbody tr { border-bottom: 1px solid rgba(255,255,255,.04); transition: background .08s; }
    #branch-popover tbody tr:last-child { border-bottom: none; }
    #branch-popover tbody tr:hover { background: var(--bg-hover); }
    #branch-popover td { padding: 7px 14px; vertical-align: middle; }
    #branch-popover td.test-name { font-family: var(--mono); font-size: 11px; color: var(--text); }
    #branch-popover td.bp-count {
      text-align: right; font-weight: 700; white-space: nowrap;
      color: #89b4fa; font-size: 12px; min-width: 36px;
    }
    #branch-popover td.bp-dir-cell { text-align: center; white-space: nowrap; }
    #branch-popover td.bp-op-cell {
      font-family: var(--mono); font-size: 11px; color: var(--text-muted);
      max-width: 140px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
    }
    #branch-popover td.bp-op-cell .arg-tag {
      display: inline-block; background: rgba(255,255,255,.06);
      border: 1px solid var(--border); border-radius: 3px;
      padding: 0 4px; margin: 1px 2px 1px 0; font-size: 10px;
      max-width: 120px; overflow: hidden; text-overflow: ellipsis;
      white-space: nowrap; vertical-align: middle;
    }
    #branch-popover .bp-dir-chip {
      display: inline-flex; align-items: center; justify-content: center;
      width: 18px; height: 18px; border-radius: 3px; font-size: 10px;
      font-weight: 700; font-family: var(--mono);
    }
    #branch-popover .bp-dir-chip.t { background: rgba(46,204,113,.25); color: #a6e3a1; border: 1px solid rgba(46,204,113,.4); }
    #branch-popover .bp-dir-chip.f { background: rgba(231,76,60,.25); color: #f38ba8; border: 1px solid rgba(231,76,60,.4); }
    #branch-popover .bp-empty-row td {
      text-align: center; color: var(--text-dim); padding: 14px; font-size: 11px;
    }
  </style>
</head>
<body>
<@topbar.render model=report.topBar />
<@nav.render report=report />
<main id="main">
  <div class="empty-state">
    <h2>Select a class</h2>
    <p>Choose a file from the tree to view coverage details.</p>
  </div>
</main>
<@popovers.render />
<script>
/* ===== LRU Cache ===== */
class LRUCache {
  constructor(maxSize) {
    this.maxSize = maxSize;
    this.map = new Map();
  }
  get(key) {
    if (!this.map.has(key)) return null;
    const val = this.map.get(key);
    this.map.delete(key);
    this.map.set(key, val);
    return val;
  }
  set(key, val) {
    if (this.map.has(key)) this.map.delete(key);
    else if (this.map.size >= this.maxSize)
      this.map.delete(this.map.keys().next().value);
    this.map.set(key, val);
  }
}

/* ===== CoverageX core object ===== */
const CoverageX = {
  cache: new LRUCache(20),
  currentId: null,
  requestedId: null,
  popovers: {},

  registerClass(id, data) {
    document.head.querySelector('script[src*="' + id + '.data.js"]')?.remove();
    this.cache.set(id, data);
    if (id !== this.requestedId) return;
    this._render(id, data);
  },

  load(id, payloadPath) {
    this.requestedId = id;
    if (id === this.currentId) return;
    const cached = this.cache.get(id);
    if (cached) { this._render(id, cached); return; }
    this._showSpinner();
    const s = document.createElement('script');
    // payloadPath comes from data-payload on the nav item (scoped reports use
    // classes/<scopeId>/<sectionId>.data.js). Fall back to the flat layout when
    // the caller did not pass one — e.g. a bookmarked call from user code.
    s.src = payloadPath || ('classes/' + id + '.data.js');
    s.onerror = () => {
      if (this.requestedId === id) this._showError(id);
    };
    document.head.appendChild(s);
  },

  _render(id, data) {
    this.currentId = id;
    this.popovers = {};
    document.getElementById('main').innerHTML = this._buildHtml(id, data);
    setActiveNav(id);
    closePopovers();
  },

  _buildHtml(id, data) {
    return this._buildHeader(id, data)
      + this._buildInsights(data.insights, id)
      + '<div class="class-body">'
      + (data.lines ? this._buildSourceTable(data.lines, id) : '')
      + (data.methods ? this._buildMethodFallback(data.methods) : '')
      + '</div>';
  },

  _buildHeader(id, data) {
    const pct = data.pct;
    const pctClass = pct >= 80 ? 'high' : pct >= 50 ? 'mid' : pct > 0 ? 'low' : 'zero';
    const fillClass = pct >= 80 ? 'fill-high' : pct >= 50 ? 'fill-mid' : pct > 0 ? 'fill-low' : 'fill-zero';
    const fillW = Math.max(2, Math.min(100, Math.round(pct)));
    let badges = '';
    if (data.crits) badges += '<span class="badge crit">✕ ' + data.crits + ' critical</span>';
    if (data.warns) badges += '<span class="badge warn">⚠ ' + data.warns + ' warnings</span>';
    if (data.infos) badges += '<span class="badge info">i ' + data.infos + ' info</span>';
    if (data.pos)   badges += '<span class="badge pos">✓ ' + data.pos + ' optimal</span>';
    return '<div class="class-section">'
      + '<div class="class-header">'
      + '<div class="class-name">' + esc(data.name) + '<span class="package-name"> — ' + esc(data.pkg) + '</span></div>'
      + '<div class="coverage-bar-wrap">'
      + '<div class="coverage-bar"><div class="coverage-bar-fill ' + fillClass + '" style="width:' + fillW + '%"></div></div>'
      + '<span class="coverage-bar-pct ' + pctClass + '">' + pct.toFixed(1) + '%</span>'
      + '</div>'
      + badges
      + '</div>';
  },

  _buildInsights(insights, sectionId) {
    if (!insights || !insights.length) return '';
    const icons = {C:'✕', W:'!', I:'i', P:'✓'};
    const rows = insights.map(ins => {
      const lineRef = ins.line > 0
        ? '<span class="line-ref" onclick="jumpToLine(\'' + esc(sectionId) + '\',' + ins.line + ')">line ' + ins.line + '</span>'
        : '';
      return '<div class="insight ' + esc(ins.sev) + '">'
        + '<span class="icon">' + (icons[ins.sev] || '?') + '</span>'
        + '<div class="body"><div class="title">' + esc(ins.msg) + '</div>'
        + '<div class="detail">' + esc(ins.hint) + '</div></div>'
        + lineRef + '</div>';
    }).join('');
    return '<div class="insights-panel">' + rows + '</div>';
  },

  _buildSourceTable(lines, sectionId) {
    const covClass = {0:'',1:'hit',2:'miss',3:'branch-partial'};
    let html = '<table class="source-view"><tbody>';
    for (const line of lines) {
      if (line.sep === 1) {
        html += '<tr class="method-separator"><td colspan="3"></td></tr>';
      }
      const rc = covClass[line.c] || '';
      const rowClass = rc ? 'line ' + rc : 'line';
      const anchorId = sectionId + '-L' + line.n;
      html += '<tr class="' + rowClass + '" id="' + anchorId + '" data-line="' + line.n + '">';
      html += '<td class="ln">' + line.n + '</td>';
      html += '<td class="hits">' + esc(line.h || '') + '</td>';
      // Source cell: escaped source text + method marker + branch badges
      let src = '<code>' + esc(line.s ?? '') + '</code>';
      if (line.mm) {
        const mm = line.mm;
        const pid = 'mm-' + line.n;
        const t = mm.t ?? 0;
        if (t === 0) {
          src += '<span class="dead-label">✗ never called</span>';
        } else {
          src += '<span class="method-marker" data-pid="' + pid + '" onclick="showInvocationPopover(event,this)">'
            + 'entry · ' + t + 'x ▾</span>';
        }
        this.popovers[pid] = {
          kind: 'method',
          method: mm.n,
          total: t,
          parameterNames: mm.params || [],
          invocations: (mm.inv || []).map(inv => ({
            tests: inv.ts || [],
            args: inv.a || [],
            count: inv.cnt
          }))
        };
      }
      if (line.bb) {
        const covCls = {0:'not-covered', 1:'semi-covered', 2:'covered'};
        line.bb.forEach((badge, i) => {
          const pid = 'bb-' + line.n + '-' + i;
          src += '<span class="branch ' + (covCls[badge.cov] || 'not-covered') + '" data-pid="' + pid
            + '" onclick="showBranchPopover(event,this)">'
            + (badge.d === 1 ? 'T' : 'F') + '</span>';
          this.popovers[pid] = {
            kind: 'branch',
            direction: badge.d === 1 ? 'TRUE' : 'FALSE',
            conditions: badge.conds.map(c => ({
              conditionText: c.t,
              trueHit:  c.th === 1,
              falseHit: c.fh === 1,
              trueCount:  c.tc ?? 0,
              falseCount: c.fc ?? 0,
              trueHint:  c.thi || '',
              falseHint: c.fhi || '',
              trueTests:  (c.tt || []).map(t => ({ testMethodName: t.t, count: t.cnt, argValues: t.args || [] })),
              falseTests: (c.ft || []).map(t => ({ testMethodName: t.t, count: t.cnt, argValues: t.args || [] })),
              operandArgs: c.ops || []
            }))
          };
        });
      }
      html += '<td class="src">' + src + '</td>';
      html += '</tr>';
    }
    html += '</tbody></table>';
    return html;
  },

  _buildMethodFallback(methods) {
    if (!methods || !methods.length) return '<div class="no-source">No source file available.</div>';
    const rows = methods.map(m =>
      '<div class="method-row"><span class="mr-name">' + esc(m.n) + '</span>'
      + '<span class="' + (m.h ? 'mr-hit' : 'mr-miss') + '">' + esc(m.st) + '</span></div>'
    ).join('');
    return '<div class="method-list">' + rows + '</div>';
  },

  _showSpinner() {
    document.getElementById('main').innerHTML = '<div class="loading-spinner">Loading…</div>';
  },

  _showError(id) {
    document.getElementById('main').innerHTML = '<div class="load-error">Failed to load class: ' + esc(id) + '</div>';
  }
};

/* ===== Popover shared state ===== */
let openPopover = null;
function closePopovers() {
  document.querySelectorAll('.popover').forEach(p => p.style.display = 'none');
  openPopover = null;
}
document.addEventListener('click', e => {
  if (openPopover && !openPopover.contains(e.target) &&
      !e.target.closest('.method-marker') && !e.target.closest('.branch')) {
    closePopovers();
  }
});
document.addEventListener('keydown', e => { if (e.key === 'Escape') closePopovers(); });

function positionPopover(popover, trigger) {
  popover.style.display = 'block';
  const tr = trigger.getBoundingClientRect();
  const pw = popover.offsetWidth, ph = popover.offsetHeight;
  const vw = window.innerWidth, vh = window.innerHeight;
  let left = tr.left, top = tr.bottom + 8;
  if (left + pw > vw - 12) left = vw - pw - 12;
  if (left < 8) left = 8;
  if (top + ph > vh - 12) top = tr.top - ph - 8;
  popover.style.left = left + 'px'; popover.style.top = top + 'px';
  const arrow = popover.querySelector('.popover-arrow');
  if (arrow) { const al = Math.max(8, tr.left + tr.width / 2 - left - 6); arrow.style.left = al + 'px'; }
}

/* ===== Invocation popover ===== */
function showInvocationPopover(event, trigger) {
  event.stopPropagation();
  const pop = document.getElementById('invocation-popover');
  if (openPopover === pop) { closePopovers(); return; }
  closePopovers();

  const data = CoverageX.popovers[trigger.dataset.pid];
  const invocations = data.invocations;
  const methodName  = data.method;
  const totalCalls  = data.total;

  const allTests    = new Set(invocations.flatMap(i => i.tests));
  const uniqueTests = allTests.size;
  const uniqueArgs  = new Set(invocations.map(i => JSON.stringify(i.args))).size;

  document.getElementById('inv-title').textContent    = methodName + '()';
  document.getElementById('inv-subtitle').textContent = totalCalls + ' total invocations';
  document.getElementById('inv-unique-tests').textContent = uniqueTests;
  document.getElementById('inv-unique-args').textContent  = uniqueArgs;

  const footerNote = document.getElementById('inv-footer-note');
  footerNote.textContent = uniqueTests > 0
    ? uniqueTests + ' test(s) covered this method'
    : 'Test tracking requires the CoverageX test listener.';

  const paramNames = data.parameterNames || [];

  // Rebuild per-parameter header columns.
  const headRow = document.getElementById('inv-thead-row');
  headRow.querySelectorAll('th.inv-param-th').forEach(el => el.remove());
  if (paramNames.length > 0) {
    paramNames.forEach(label => {
      const th = document.createElement('th');
      th.className = 'inv-param-th';
      th.textContent = label;
      headRow.appendChild(th);
    });
  } else {
    const th = document.createElement('th');
    th.className = 'inv-param-th inv-args-fallback';
    th.textContent = 'Arguments';
    headRow.appendChild(th);
  }

  const tbody = document.getElementById('inv-tbody');
  tbody.innerHTML = invocations.map(inv => {
    const testLabel = inv.tests.length ? inv.tests.join(', ') : '';
    if (paramNames.length > 0) {
      const paramCells = paramNames.map((_, i) => renderInvParamCell((inv.args || [])[i])).join('');
      return '<tr>'
        + '<td class="test-name">' + esc(testLabel) + '</td>'
        + '<td class="inv-count">' + inv.count + '×</td>'
        + paramCells
        + '</tr>';
    }
    const args = (inv.args || []).map(a => {
      if (a === null)  return '<span class="arg-tag" title="&lt;null&gt;">&lt;null&gt;</span>';
      if (a === '')    return '<span class="arg-tag" title="&lt;empty&gt;">&lt;empty&gt;</span>';
      const objRef = /^([\w.$]+)@[0-9a-f]+$/i.exec(a);
      if (objRef) return '<span class="arg-tag" title="' + escAttr(a) + '">' + esc(truncate(objRef[1], 22)) + '</span>';
      return '<span class="arg-tag" title="' + escAttr(a) + '">' + esc(truncate(a, 22)) + '</span>';
    }).join('');
    return '<tr>'
      + '<td class="test-name">' + esc(testLabel) + '</td>'
      + '<td class="inv-count">' + inv.count + '×</td>'
      + '<td class="inv-args">' + (args || '<span style="color:var(--text-dim)">--</span>') + '</td>'
      + '</tr>';
  }).join('');

  openPopover = pop;
  positionPopover(pop, trigger);
}

/* ===== Branch popover ===== */
let _bpConditions = [];
function showBranchPopover(event, trigger) {
  event.stopPropagation();
  const pop = document.getElementById('branch-popover');
  if (openPopover === pop && pop._trigger === trigger) { closePopovers(); return; }
  closePopovers();

  const data = CoverageX.popovers[trigger.dataset.pid];
  const conditions = data.conditions;
  const direction  = data.direction;
  _bpConditions = conditions;

  document.getElementById('bp-line-label').textContent = 'line ' + trigger.closest('tr').dataset.line;
  const defaultIdx = conditions.findIndex(c => direction === 'TRUE' ? !c.trueHit : !c.falseHit);
  const selectedIdx = defaultIdx === -1 ? 0 : defaultIdx;
  if (conditions.length === 1) {
    document.getElementById('bp-condition-selector').style.display = 'none';
    document.getElementById('bp-condition-single').style.display = '';
    document.getElementById('bp-condition-text').textContent = conditions[0].conditionText;
  } else {
    document.getElementById('bp-condition-single').style.display = 'none';
    document.getElementById('bp-condition-selector').style.display = '';
    const sel = document.getElementById('bp-condition-select');
    sel.innerHTML = conditions.map((c, i) => '<option value="' + i + '">' + esc(c.conditionText) + '</option>').join('');
    sel.selectedIndex = selectedIdx;
  }
  renderConditionPanels(conditions[selectedIdx]);
  pop._trigger = trigger;
  openPopover = pop;
  positionPopover(pop, trigger);
}

function onBranchConditionChange() {
  const idx = parseInt(document.getElementById('bp-condition-select').value, 10);
  renderConditionPanels(_bpConditions[idx]);
}

function renderConditionPanels(cond) {
  const trueDir = document.getElementById('bp-true-dir');
  trueDir.className = 'bp-dir ' + (cond.trueHit ? 'hit' : 'miss');
  document.getElementById('bp-true-status').textContent = cond.trueHit ? '✓ taken' : '✗ never';
  document.getElementById('bp-true-count').textContent  = cond.trueHit ? cond.trueCount + '× hit' : 'not covered';
  const falseDir = document.getElementById('bp-false-dir');
  falseDir.className = 'bp-dir ' + (cond.falseHit ? 'hit' : 'miss');
  document.getElementById('bp-false-status').textContent = cond.falseHit ? '✓ taken' : '✗ never';
  document.getElementById('bp-false-count').textContent  = cond.falseHit ? cond.falseCount + '× hit' : 'not covered';
  const hintRow  = document.getElementById('bp-hint-row');
  const hintIcon = document.getElementById('bp-hint-icon');
  const hintText = document.getElementById('bp-hint-text');
  const hint = !cond.trueHit ? cond.trueHint : (!cond.falseHit ? cond.falseHint : cond.trueHint);
  if (hint) {
    hintRow.style.display = '';
    const bothCovered = cond.trueHit && cond.falseHit;
    hintIcon.textContent = bothCovered ? '✓' : '💡';
    hintIcon.style.color = bothCovered ? 'var(--badge-pos)' : 'var(--badge-warn)';
    hintText.innerHTML = esc(hint);
  } else { hintRow.style.display = 'none'; }
  renderBranchTestsTable(cond);
}

function renderBranchTestsTable(cond) {
  const trueTests  = cond.trueTests  || [];
  const falseTests = cond.falseTests || [];
  const ops        = cond.operandArgs || [];

  const uniqueTests = new Set();
  trueTests.forEach(t => uniqueTests.add(t.testMethodName));
  falseTests.forEach(t => uniqueTests.add(t.testMethodName));
  const dirsCovered = (cond.trueHit ? 1 : 0) + (cond.falseHit ? 1 : 0);

  document.getElementById('bp-unique-tests').textContent = uniqueTests.size;
  document.getElementById('bp-directions-covered').textContent = dirsCovered + '/2';

  const footerNote = document.getElementById('bp-footer-note');
  footerNote.textContent = uniqueTests.size > 0
      ? uniqueTests.size + ' test(s) covered this branch'
      : 'Test tracking requires the CoverageX test listener.';

  // Rebuild header: drop any previously-injected operand <th>s, then re-inject one per operand label.
  const headRow = document.getElementById('bp-tests-thead-row');
  headRow.querySelectorAll('th.bp-op-th').forEach(el => el.remove());
  ops.forEach(label => {
    const th = document.createElement('th');
    th.className = 'bp-op-th';
    th.textContent = label;
    headRow.appendChild(th);
  });

  const rows = [];
  trueTests.forEach(t  => rows.push({ test: t, dir: 'T' }));
  falseTests.forEach(t => rows.push({ test: t, dir: 'F' }));

  const tbody = document.getElementById('bp-tests-tbody');
  if (rows.length === 0) {
    const colspan = 3 + ops.length;
    tbody.innerHTML = '<tr class="bp-empty-row"><td colspan="' + colspan
        + '">No tests covered this branch.</td></tr>';
    return;
  }

  tbody.innerHTML = rows.map(r => {
    const dirChip = '<span class="bp-dir-chip ' + r.dir.toLowerCase() + '">'
        + r.dir + '</span>';
    const values = r.test.argValues || [];
    const opCells = ops.map((_, i) => renderOperandCell(values[i])).join('');
    return '<tr>'
        + '<td class="test-name">' + esc(r.test.testMethodName) + '</td>'
        + '<td class="bp-count">' + r.test.count + '×</td>'
        + '<td class="bp-dir-cell">' + dirChip + '</td>'
        + opCells
        + '</tr>';
  }).join('');
}

function renderOperandCell(value) {
  if (value === undefined) {
    return '<td class="bp-op-cell"><span style="color:var(--text-dim)">--</span></td>';
  }
  if (value === null) {
    return '<td class="bp-op-cell"><span class="arg-tag" title="&lt;null&gt;">&lt;null&gt;</span></td>';
  }
  if (value === '') {
    return '<td class="bp-op-cell"><span class="arg-tag" title="&lt;empty&gt;">&lt;empty&gt;</span></td>';
  }
  return '<td class="bp-op-cell"><span class="arg-tag" title="' + escAttr(value) + '">'
      + esc(truncate(value, 22)) + '</span></td>';
}

/**
 * Renders one per-parameter cell in the invocation popover table.
 * Mirrors {@link renderOperandCell} but uses the {@code inv-param-cell} CSS class.
 */
function renderInvParamCell(value) {
  if (value === undefined) {
    return '<td class="inv-param-cell"><span style="color:var(--text-dim)">--</span></td>';
  }
  if (value === null) {
    return '<td class="inv-param-cell"><span class="arg-tag" title="&lt;null&gt;">&lt;null&gt;</span></td>';
  }
  if (value === '') {
    return '<td class="inv-param-cell"><span class="arg-tag" title="&lt;empty&gt;">&lt;empty&gt;</span></td>';
  }
  const objRef = /^([\w.$]+)@[0-9a-f]+$/i.exec(value);
  if (objRef) {
    return '<td class="inv-param-cell"><span class="arg-tag" title="' + escAttr(value) + '">'
        + esc(truncate(objRef[1], 22)) + '</span></td>';
  }
  return '<td class="inv-param-cell"><span class="arg-tag" title="' + escAttr(value) + '">'
      + esc(truncate(value, 22)) + '</span></td>';
}

/* ===== Shared helpers ===== */
function esc(s) {
  return String(s).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');
}
function escAttr(s) { return esc(s); }
function truncate(s, n) { return s.length > n ? s.slice(0, n) + '…' : s; }

/* ===== Jump to line ===== */
function jumpToLine(sectionId, lineNum) {
  const el = document.getElementById(sectionId + '-L' + lineNum);
  if (el) {
    el.scrollIntoView({ behavior: 'smooth', block: 'center' });
    el.style.outline = '2px solid #89b4fa';
    setTimeout(() => el.style.outline = '', 1400);
  }
}

/* ===== Jump to next uncovered ===== */
let jumpIndex = 0;
function jumpNextUncovered() {
  const targets = [...document.querySelectorAll('#main .line.miss, #main .line.branch-partial')];
  if (!targets.length) return;
  jumpIndex = jumpIndex % targets.length;
  const el = targets[jumpIndex++];
  el.scrollIntoView({ behavior: 'smooth', block: 'center' });
  el.style.outline = '2px solid #89b4fa';
  setTimeout(() => el.style.outline = '', 1200);
}

/* ===== Load class entry point ===== */
function loadClass(id, payloadPath) {
  CoverageX.load(id, payloadPath);
}

/* ===== Prev / Next navigation ===== */
function getClassOrder() {
  return [...document.querySelectorAll('#nav-list .nav-item[data-section]')]
    .map(el => el.dataset.section);
}
function navigateRelative(delta) {
  const items = [...document.querySelectorAll('#nav-list .nav-item[data-section]')];
  const idx = items.findIndex(el => el.dataset.section === CoverageX.currentId);
  const next = idx + delta;
  if (next >= 0 && next < items.length) {
    loadClass(items[next].dataset.section, items[next].dataset.payload);
  }
}

/* ===== Tree navigation ===== */
function toggleNavFolder(row) {
  row.closest('.nav-folder').classList.toggle('open');
}

function setActiveNav(id) {
  document.querySelectorAll('#nav-list .nav-item').forEach(el => el.classList.remove('active'));
  const el = document.querySelector('#nav-list .nav-item[data-section="' + id + '"]');
  if (!el) return;
  el.classList.add('active');
  let p = el.parentElement;
  while (p && p.id !== 'nav-list') {
    if (p.classList.contains('nav-folder')) p.classList.add('open');
    p = p.parentElement;
  }
  el.scrollIntoView({ block: 'nearest' });
}

function filterNav(q) {
  const query = q.trim().toLowerCase();
  const allFiles   = document.querySelectorAll('#nav-list .nav-item');
  const allFolders = document.querySelectorAll('#nav-list .nav-folder');
  if (!query) {
    allFiles.forEach(el => el.style.display = '');
    allFolders.forEach(el => {
      el.style.display = '';
      const fc = el.querySelector(':scope > .nav-folder-children');
      if (fc) fc.style.display = '';
    });
    return;
  }
  allFolders.forEach(el => el.style.display = 'none');
  allFiles.forEach(el => el.style.display = 'none');
  allFiles.forEach(el => {
    if (el.querySelector('.nav-name').textContent.toLowerCase().includes(query)) {
      el.style.display = '';
      let p = el.parentElement;
      while (p && p.id !== 'nav-list') {
        if (p.classList.contains('nav-folder-children')) p.style.display = 'block';
        if (p.classList.contains('nav-folder')) p.style.display = '';
        p = p.parentElement;
      }
    }
  });
}

let activeSeverity = null;
function filterSeverity(sev, btn) {
  const dataAttr = 'data-has-' + sev;
  if (activeSeverity === sev) {
    activeSeverity = null;
    document.querySelectorAll('.filter-chip').forEach(c => c.className = 'filter-chip');
    document.querySelectorAll('#nav-list .nav-item').forEach(i => i.style.display = '');
    document.querySelectorAll('#nav-list .nav-folder').forEach(el => {
      el.style.display = '';
      const fc = el.querySelector(':scope > .nav-folder-children');
      if (fc) fc.style.display = '';
    });
  } else {
    activeSeverity = sev;
    document.querySelectorAll('.filter-chip').forEach(c => c.className = 'filter-chip');
    btn.classList.add('active-' + sev);
    document.querySelectorAll('#nav-list .nav-folder').forEach(el => el.style.display = 'none');
    document.querySelectorAll('#nav-list .nav-item').forEach(el => el.style.display = 'none');
    document.querySelectorAll('#nav-list .nav-item[' + dataAttr + '="1"]').forEach(el => {
      el.style.display = '';
      let p = el.parentElement;
      while (p && p.id !== 'nav-list') {
        if (p.classList.contains('nav-folder-children')) p.style.display = 'block';
        if (p.classList.contains('nav-folder')) p.style.display = '';
        p = p.parentElement;
      }
    });
  }
}

/* ===== Topbar button wiring ===== */
const btnJump = document.getElementById('btn-jump');
if (btnJump) btnJump.addEventListener('click', jumpNextUncovered);

/* ===== Nav sidebar resize & collapse ===== */
(function () {
  const NAV_MIN = 140, NAV_MAX = 640, NAV_DEFAULT = 260;
  let navW = parseInt(localStorage.getItem('navWidth') || NAV_DEFAULT, 10);
  let collapsed = localStorage.getItem('navCollapsed') === '1';

  function applyState(animate) {
    const w = collapsed ? 0 : Math.max(NAV_MIN, Math.min(NAV_MAX, navW));
    if (animate) document.documentElement.style.setProperty('--nav-transition', 'grid-template-columns .2s');
    document.documentElement.style.setProperty('--nav-w', w + 'px');
    if (animate) setTimeout(() => document.documentElement.style.removeProperty('--nav-transition'), 220);
    const btn = document.getElementById('nav-toggle');
    if (btn) {
      btn.textContent = collapsed ? '≫' : '☰';
      btn.title = collapsed ? 'Show sidebar' : 'Hide sidebar';
    }
  }

  // Apply on load (no animation)
  applyState(false);

  document.getElementById('nav-toggle')?.addEventListener('click', () => {
    collapsed = !collapsed;
    localStorage.setItem('navCollapsed', collapsed ? '1' : '0');
    applyState(true);
  });

  const resizer = document.getElementById('nav-resizer');
  if (resizer) {
    resizer.addEventListener('mousedown', e => {
      if (collapsed) return;
      e.preventDefault();
      const startX = e.clientX;
      const startW = navW;
      resizer.classList.add('dragging');
      document.body.classList.add('nav-resizing');

      function onMove(e) {
        navW = Math.max(NAV_MIN, Math.min(NAV_MAX, startW + e.clientX - startX));
        document.documentElement.style.setProperty('--nav-w', navW + 'px');
      }
      function onUp() {
        resizer.classList.remove('dragging');
        document.body.classList.remove('nav-resizing');
        localStorage.setItem('navWidth', navW);
        document.removeEventListener('mousemove', onMove);
        document.removeEventListener('mouseup', onUp);
      }
      document.addEventListener('mousemove', onMove);
      document.addEventListener('mouseup', onUp);
    });
  }
})();
</script>
</body>
</html>
