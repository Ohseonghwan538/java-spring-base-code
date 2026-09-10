function mouseOverInterface(_isOver) {
    if (typeof Module == "object") {
        Module.XDIsMouseOverDiv(_isOver);
    }
}

window.onresize = function(e) {
    if (typeof Module == 'object') {
        if (typeof Module.Resize == 'function') {
            Module.Resize(window.innerWidth, window.innerHeight);
            Module.XDRenderData();
        }
    }
};

var Module = {
    locateFile: function(s) {
        return "./src/js/engine/" + s;
    },
    postRun: function() {
        // 엔진 초기화 API 호출
        Module.initialize({
            container: document.getElementById("map"),
            terrain: {
                dem: {
                    url: "https://xdworld.vworld.kr",
                    name: "dem",
                    servername: "XDServer3d",
                    encoding: true
                },
                image: {
                    url: "https://xdworld.vworld.kr",
                    name: "tile",
                    servername: "XDServer3d"
                },
            },
            worker: {
                use: true,
                path: "./src/js/worker/XDWorldWorker.js",
                count: 5
            },
            defaultKey: "eza2eBBqd!Hmd!JQ45QpEpB~#Fb!EQBmeFDP4FEPDzb1dFg21I=="
        });
        
        // 엔진 초기화 완료 이벤트 발신
        window.dispatchEvent(new CustomEvent("XDWorldLoaded"));
    }
};


