package com.dergoogler.mmrl.webui.handler

import android.util.Log
import com.dergoogler.mmrl.webui.PathHandler
import com.dergoogler.mmrl.webui.asScriptResponse
import com.dergoogler.mmrl.webui.asStyleResponse
import com.dergoogler.mmrl.webui.model.Insets
import com.dergoogler.mmrl.webui.model.WebColors
import com.dergoogler.mmrl.webui.notFoundResponse
import com.dergoogler.mmrl.webui.util.WebUIOptions
import java.io.IOException

fun internalPathHandler(
    options: WebUIOptions,
    insets: Insets
): PathHandler {
    val colorScheme = options.colorScheme
    val webColors = WebColors(colorScheme)

    val assetsHandler = assetsPathHandler(options)

    return handler@{ path ->
        try {
            if (path.matches(Regex("^assets(/.*)?$"))) {
                return@handler assetsHandler(path.removePrefix("assets/"))
            }

            val inputStream = options.modId.sanitizedIdWithFileInputStream

            if (path.matches(Regex("scripts/require\\.js"))) {
                return@handler """(function() {
    // Configuration
    var BASE_MODULE_PATH = '/data/adb/modules';
    var CURRENT_MODULE_ID = '${options.modId.id}';
    
    // Module cache
    var moduleCache = {};
    
    // Get current working directory
    function getCwd() {
        return BASE_MODULE_PATH + '/' + CURRENT_MODULE_ID + '/webroot/';
    }
    
    // Module constructor
    function Module(id) {
        this.id = id;
        this.exports = {};
    }
    
    // Convert byte array to string
    function bytesToString(byteArray) {
        return String.fromCharCode.apply(null, byteArray);
    }
    
    // Resolve a path relative to cwd
    function resolvePath(relativePath) {
        if (relativePath.charAt(0) === '/') {
            return relativePath; // Absolute path
        }
        return getCwd() + relativePath;
    }
    
    // Resolve module path
    function resolveModulePath(moduleId) {
        // Handle absolute paths
        if (moduleId.charAt(0) === '/') {
            return [moduleId, moduleId + '.js'];
        }
        
        // Handle relative paths
        if (moduleId.indexOf('./') === 0 || moduleId.indexOf('../') === 0) {
            var basePath = resolvePath(moduleId);
            return [
                basePath,
                basePath + '.js',
                basePath + '/index.js'
            ];
        }
        
        // Handle module paths
        var parts = moduleId.split('/');
        var moduleBase, subPath;
        
        if (moduleId.charAt(0) === '@') {
            moduleBase = parts.slice(0, 2).join('/');
            subPath = parts.slice(2).join('/');
        } else {
            moduleBase = parts[0];
            subPath = parts.slice(1).join('/');
        }
        
        // Generate paths to try
        var pathsToTry = [];
        var baseModulePath = BASE_MODULE_PATH + '/' + moduleBase + '/webroot/';
        
        if (subPath) {
            pathsToTry.push(baseModulePath + subPath);
            pathsToTry.push(baseModulePath + subPath + '.js');
            pathsToTry.push(baseModulePath + subPath + '/index.js');
        } else {
            pathsToTry.push(baseModulePath + 'index.js');
        }
        
        return pathsToTry;
    }
    
    // File system loader using InputStream
    function loadFromFileSystem(filePath) {
        try {
            var input = $inputStream.open(filePath);
            var result = [];
            
            while (true) {
                var byte = input.read();
                if (byte === -1) break;
                result.push(byte);
            }
            
            input.close();
            return result;
        } catch (error) {
            throw new Error('File read error for ' + filePath + ': ' + error.message);
        }
    }
    
    // Load text file (for HTML includes)
    function loadTextFile(filePath) {
        try {
            var bytes = loadFromFileSystem(filePath);
            return bytesToString(bytes);
        } catch (e) {
            console.error('Failed to load file: ' + filePath, e);
            return 'Error loading ' + filePath;
        }
    }
    
    // HTML includes functionality
    function resolveIncludePath(relativePath) {
        return resolvePath(relativePath);
    }
    
    function loadHtmlIncludes() {
        var elements = document.querySelectorAll('[data-include]');
        for (var i = 0; i < elements.length; i++) {
            var el = elements[i];
            var relativePath = el.getAttribute('data-include');
            var filePath = resolveIncludePath(relativePath);
            var html = loadTextFile(filePath);
            el.innerHTML = html;
        }
    }
    
    // Main require function
    window.require = function(moduleId) {
        // Check cache first
        if (moduleCache[moduleId]) {
            return moduleCache[moduleId].exports;
        }
        
        // Create new module and cache it
        var module = new Module(moduleId);
        moduleCache[moduleId] = module;
        
        // Get possible paths to try
        var pathsToTry = resolveModulePath(moduleId);
        var lastError = null;
        
        for (var i = 0; i < pathsToTry.length; i++) {
            var filePath = pathsToTry[i];
            try {
                // Load module content
                var byteArray = loadFromFileSystem(filePath);
                var moduleCode = bytesToString(byteArray);
                
                // Create wrapper function
                var wrapperFn = new Function(
                    'module', 'exports', 'require', 
                    moduleCode + '\n//# sourceURL=' + filePath
                );
                
                // Execute the module
                wrapperFn.call(
                    module.exports, 
                    module, 
                    module.exports, 
                    require
                );
                
                return module.exports;
            } catch (error) {
                lastError = error;
                continue;
            }
        }
        
        // Clean up cache on error
        delete moduleCache[moduleId];
        throw new Error('Failed to load module "' + moduleId + '". Tried paths:\n' + pathsToTry.join('\n') + '\nLast error: ' + (lastError ? lastError.message : 'unknown'));
    };
    
    // Support for extension-less requires
    var originalRequire = window.require;
    window.require = function(moduleId) {
        try {
            return originalRequire(moduleId);
        } catch (e) {
            if (moduleId.slice(-3) !== '.js') {
                try {
                    return originalRequire(moduleId + '.js');
                } catch (e2) {
                    throw e;
                }
            }
            throw e;
        }
    };
    
    // Initialize HTML includes when DOM is ready
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', loadHtmlIncludes);
    } else {
        loadHtmlIncludes();
    }
})();
""".trimIndent().asScriptResponse()
            }

            if (path.matches(Regex("insets\\.css"))) {
                return@handler insets.cssResponse
            }

            if (path.matches(Regex("colors\\.css"))) {
                return@handler webColors.allCssColors.asStyleResponse()
            }

            return@handler notFoundResponse
        } catch (e: IOException) {
            Log.e("mmrlPathHandler", "Error opening mmrl asset path: $path", e)
            return@handler notFoundResponse
        }
    }
}