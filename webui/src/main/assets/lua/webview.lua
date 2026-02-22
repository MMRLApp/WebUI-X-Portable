require("wx:std")
require("wx:str")

local R                 = java.import("android.R")
local WX_R              = java.import("com.dergoogler.mmrl.webui.R")
local HWUI_R            = java.import("com.dergoogler.mmrl.hybridwebui.R")
local Uri               = java.import("android.net.Uri")

_G.WebResourceResponse  = java.import("android.webkit.WebResourceResponse")
_G.WebViewFeature       = java.import("androidx.webkit.WebViewFeature")
_G.WebMessageCompat     = java.import("androidx.webkit.WebMessageCompat")

local rootView          = context:getWindow():getDecorView():getRootView()
local wxView            = rootView:findViewById(WX_R.id.wxview)

local M = {}

-- M.console               = wxView:console

function M.registerPathHandler(path, handler, authority)
    if authority == nil then
        authority = domain:toString()
    end

    local handlerProxy = java.proxy("com.dergoogler.mmrl.hybridwebui.HybridWebUI$PathHandler", handler)
    wxView:addPathHandler(path, handlerProxy, Uri:parse(authority))
end

function M.registerEventListener(objectName, listener)
    local listenerProxy = java.proxy("com.dergoogler.mmrl.hybridwebui.HybridWebUI$EventListener", listener)
    wxView:addEventListener(objectName, listenerProxy)
end

-- Response helpers

function M.htmlResponse(body, status)
    return WebResourceResponse("text/html",        "UTF-8", status or 200, "OK", {}, stringToStream(body))
end

function M.jsonResponse(body, status)
    return WebResourceResponse("application/json", "UTF-8", status or 200, "OK", {}, stringToStream(body))
end

function M.textResponse(body, status)
    return WebResourceResponse("text/plain",       "UTF-8", status or 200, "OK", {}, stringToStream(body))
end

function M.errorResponse(code, message)
    return WebResourceResponse("text/plain",       "UTF-8", code, message,  {} , stringToStream(message))
end


return M