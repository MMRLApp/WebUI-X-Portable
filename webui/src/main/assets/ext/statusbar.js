function checkTopContrast() {
    const canvas = document.createElement('canvas');
    const ctx = canvas.getContext('2d', { willReadFrequently: true });
    canvas.width = 1;
    canvas.height = 27;

    ctx.drawWindow ? ctx.drawWindow(window, 0, 0, 1, 27, "white") : null;

    const topElement = document.elementFromPoint(5, 5);
    const bgColor = window.getComputedStyle(topElement).backgroundColor;

    const rgb = bgColor.match(/\d+/g).map(Number);

    const a = rgb.map(v => {
        v /= 255;
        return v <= 0.03928 ? v / 12.92 : Math.pow((v + 0.055) / 1.055, 2.4);
    });
    const luminance = 0.2126 * a[0] + 0.7152 * a[1] + 0.0722 * a[2];

    if (window.webui) {
        window.webui.updateStatusBarIconTint(luminance > 0.5);
    }
}

window.addEventListener('load', checkTopContrast);
window.addEventListener('scroll', checkTopContrast);