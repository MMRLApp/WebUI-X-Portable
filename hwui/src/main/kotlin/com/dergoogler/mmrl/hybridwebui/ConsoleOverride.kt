package com.dergoogler.mmrl.hybridwebui

internal const val FORMAT_VALUE_JS = """function __fv(val, depth) {
    if (depth === undefined) depth = 0;
    if (val === undefined) return { type: 'primitive', kind: 'undefined', value: 'undefined' };
    if (val === null)      return { type: 'primitive', kind: 'null',      value: 'null' };
    if (typeof val === 'boolean') return { type: 'primitive', kind: 'boolean', value: String(val) };
    if (typeof val === 'number')  return { type: 'primitive', kind: 'number',  value: String(val) };
    if (typeof val === 'string')  return { type: 'primitive', kind: 'string',  value: val };
    if (typeof val === 'symbol')  return { type: 'primitive', kind: 'other',   value: val.toString() };
    if (typeof val === 'function') {
        var src = Function.prototype.toString.call(val);
        var sig = src.split('\n')[0].replace(/\{.*/, '').trim();
        return { type: 'primitive', kind: 'function', value: 'f ' + sig };
    }
    if (Array.isArray(val)) {
        if (depth >= 3) return { type: 'primitive', kind: 'other', value: '[Array(' + val.length + ')]' };
        var items = [];
        for (var i = 0; i < Math.min(val.length, 50); i++) {
            try { var c = __fv(val[i], depth + 1); c.key = String(i); items.push(c); }
            catch (e) { items.push({ type: 'primitive', kind: 'other', value: '?', key: String(i) }); }
        }
        if (val.length > 50) items.push({ type: 'primitive', kind: 'other', value: '... ' + (val.length - 50) + ' more', key: '...' });
        return { type: 'expandable', label: 'Array(' + val.length + ')', closeToken: ']', children: items };
    }
    if (typeof val === 'object') {
        if (depth >= 3) return { type: 'primitive', kind: 'other', value: '{...}' };
        var prefix = '';
        if (val.constructor && val.constructor.name && val.constructor.name !== 'Object') prefix = val.constructor.name;
        var keys = [];
        try { keys = Object.getOwnPropertyNames(val).filter(function (k) { return k !== '__proto__'; }); } catch (e) {}
        if (keys.length === 0) try { keys = Object.keys(val); } catch (e) {}
        var pairs = []; var shown = 0;
        for (var i = 0; i < keys.length && shown < 30; i++) {
            var k = keys[i];
            try { var c = __fv(val[k], depth + 1); c.key = k; pairs.push(c); shown++; }
            catch (e) { pairs.push({ type: 'primitive', kind: 'other', value: '[getter]', key: k }); shown++; }
        }
        if (keys.length > 30) pairs.push({ type: 'primitive', kind: 'other', value: '... ' + (keys.length - 30) + ' more keys', key: '...' });
        return { type: 'expandable', label: prefix, closeToken: '}', children: pairs };
    }
    return { type: 'primitive', kind: 'other', value: String(val) };
};"""

internal val CONSOLE_OVERRIDE_JS = """(function () {
    if (window.__hw_console_patched__) return;
    window.__hw_console_patched__ = true;

    var _bridge = window.__hw_web_console_internal__;
    if (!_bridge || typeof _bridge.postMessage !== 'function') return;

    $FORMAT_VALUE_JS

    function intercept(level) {
        var orig = console[level].bind(console);
        console[level] = function () {
            orig.apply(console, arguments);
            var args = Array.prototype.slice.call(arguments);
            var serialized = args.map(function (a) { return __fv(a); });
            try { _bridge.postMessage(JSON.stringify({ l: level, v: serialized })); } catch (e) {}
        };
    }

    intercept('log');
    intercept('warn');
    intercept('error');
    intercept('info');
    intercept('debug');
})();""".trimIndent()

private fun escapeJsString(code: String): String {
    val escaped = code
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
    return "\"$escaped\""
}

fun wrapConsoleEvalResult(code: String) = """(function() {
    $FORMAT_VALUE_JS
    try {
        var __r = eval(${escapeJsString(code)});
        return JSON.stringify({ ok:true, value:__fv(__r) });
    } catch(e) {
        return JSON.stringify({ ok:false, value:e.toString() });
    }
})();""".trimIndent()


val String.iife get() = "(function () {$this})();"
val String.asyncIife get() = "(async function () {$this})();"