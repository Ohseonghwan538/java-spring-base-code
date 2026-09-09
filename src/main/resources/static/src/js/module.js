var GLOBAL = {
    Layer: null,
    
    selectedImageState: {
        imageId: null,
        objectKey: null,
        textObjectKey: null,
        position: { longitude: 0, latitude: 0, altitude: 0 },
        textPosition: { longitude: 0, latitude: 0, altitude: 0 },
        memoText: ""
    },

    // 선택 상태 초기화 함수
    resetSelectedImageState: function() {
        this.selectedImageState.imageId = null;
        this.selectedImageState.objectKey = null;
        this.selectedImageState.textObjectKey = null;
        this.selectedImageState.position = { longitude: 0, latitude: 0, altitude: 0 };
        this.selectedImageState.textPosition = { longitude: 0, latitude: 0, altitude: 0 };
        this.selectedImageState.memoText = "";
    }
};

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

        // 건물 레이어 추가
        var layer = Module.getTileLayerList().createXDServerLayer({
            url: "https://xdworld.vworld.kr",
            servername: "XDServer3d",
            name: "facility_build",
            type: 9,
            minLevel: 0,
            maxLevel: 15
        });
        Module.setVisibleRange("facility_build", 1.5, 200000.0);
        
        // 빌보드를 저장할 레이어 생성
        var layerList = new Module.JSLayerList(true);
        GLOBAL.Layer = layerList.createLayer("BILLBOARD", Module.ELT_BILLBOARD);
        GLOBAL.Layer.setMaxDistance(200000.0);

        // 엔진 초기화 완료 이벤트 발신
        window.dispatchEvent(new CustomEvent("XDWorldLoaded"));
    }
};

/* 아이콘 생성 함수 */
function createIcon(_iconName, _url, _imageLoadedCallback) {
    var img = new Image();
    img.crossOrigin = "Anonymous"; // CORS 문제 방지
    img.onload = function() {
        var canvas = document.createElement('canvas');
        canvas.width = img.width;
        canvas.height = img.height;
        
        var ctx = canvas.getContext('2d');
        ctx.drawImage(img, 0, 0);
        
        var imageData = ctx.getImageData(0, 0, canvas.width, canvas.height).data;
        
        if (Module.getSymbol().insertIcon(_iconName, imageData, canvas.width, canvas.height)) {          
            if (_imageLoadedCallback) {
                _imageLoadedCallback(_iconName);
            }
        }
    };
    img.src = _url;
}

/* 고도 Offset을 적용한 3D Vector3D 좌표 생성 헬퍼 함수 */
function createPositionWithOffset(_basePosition, _altitudeOffset) {
    // _basePosition이 JSVector3D 객체이거나 일반 {longitude, latitude, altitude} 객체인 경우 모두 대응
    var lon = _basePosition.longitude || _basePosition.Longitude || _basePosition.x;
    var lat = _basePosition.latitude || _basePosition.Latitude || _basePosition.y;
    var alt = (_basePosition.altitude || _basePosition.Altitude || _basePosition.z) + _altitudeOffset;

    return new Module.JSVector3D(lon, lat, alt);
}

/* 이미지 빌보드 생성 및 배치 함수 (개선) */
function createImageBillboard(_objectKey, _position, _iconName) {
    var billboard = Module.createBillboard(_objectKey);
    
    // JSVector3D 객체 인스턴스 보장
    var pos = (_position instanceof Module.JSVector3D) ? 
              _position : new Module.JSVector3D(_position.longitude, _position.latitude, _position.altitude);

    billboard.set(pos, Module.getSymbol().getIcon(_iconName), 150.0, 150.0);
    
    // addObject의 두 번째 인자로 오브젝트 키를 지정하여 추후 선택/삭제가 가능하도록 개선
    GLOBAL.Layer.addObject(billboard, _objectKey);
}

/* 텍스트 빌보드 생성 및 배치 함수 (2단계 핵심 추가) */
function createTextBillboard(_objectKey, _position, _textOptions) {
    // 1. 이미지 빌보드 아래로 고도 Offset 적용 (-18.0m)
    var textPosition = createPositionWithOffset(_position, -18.0);

    // 2. 텍스트 빌보드 객체 생성
    var billboard = Module.createBillboard(_objectKey);
    
    // 3. 캔버스 기반 텍스트 이미지 데이터 생성
    var canvas = document.createElement("canvas");
    var boardImage = createBoardImage(canvas, _textOptions);
    
    // 4. 빌보드에 텍스트 캔버스 이미지 설정
    billboard.setImage(textPosition, boardImage.data, boardImage.width, boardImage.height);
    
    // 5. 동일한 GLOBAL.Layer에 추가
    GLOBAL.Layer.addObject(billboard, _objectKey);
    
    return billboard;
}

/* 카메라 이동 헬퍼 함수 */
function moveCameraTo(lon, lat, alt) {
    if (typeof Module !== "undefined" && Module.getViewCamera) {
        // 지정된 좌표 위로 카메라 이동
        Module.getViewCamera().move(new Module.JSVector3D(lon, lat - 0.005, alt + 200.0), 20, 0, 0);
    }
}
