class BillboardLayer {
    constructor() {
        this.layer = null;
    }

    init() {
        var layerList = new Module.JSLayerList(true);
        this.layer = layerList.createLayer("BILLBOARD", Module.ELT_BILLBOARD);

        // 빌보드 레이어의 최대 가시 거리 설정 (100km)
        this.layer.setMaxDistance(100000.0);

        return this.layer;
    }

    // Layer 가시화 
    setLayerVisible(_visible) {
        if (this.layer == null) {
            return;
        }

        this.layer.setVisible(_visible);
    }

    // Layer 삭제 (메모리 점유율 고려)
    deleteLayer() {
        if (this.layer == null) {
            return;
        }
        this.layer.delLayerAtName("BILLBOARD");
        this.layer = null;
    }

    /* 이미지 빌보드 생성 및 배치 함수 (개선) */
    createImageBillboard(_objectKey, _position, _iconName) {
        var billboard = Module.createBillboard(_objectKey);
        
        // JSVector3D 객체 인스턴스 보장
        var pos = (_position instanceof Module.JSVector3D) ? 
                _position : new Module.JSVector3D(_position.longitude, _position.latitude, _position.altitude);

        billboard.set(pos, Module.getSymbol().getIcon(_iconName), 150.0, 150.0);
        
        // addObject의 두 번째 인자로 오브젝트 키를 지정하여 추후 선택/삭제가 가능하도록 개선
        this.layer.addObject(billboard, _objectKey);
    }

    /* 텍스트 빌보드 생성 및 배치 함수 (2단계 핵심 추가) */
    createTextBillboard(_objectKey, _position, _textOptions) {
        // 1. 이미지 빌보드 아래로 고도 Offset 적용 (-18.0m)
        var textPosition = createPositionWithOffset(_position, -18.0);

        // 2. 텍스트 빌보드 객체 생성
        var billboard = Module.createBillboard(_objectKey);
        
        // 3. 캔버스 기반 텍스트 이미지 데이터 생성
        var canvas = document.createElement("canvas");
        var boardImage = createBoardImage(canvas, _textOptions);
        
        // 4. 빌보드에 텍스트 캔버스 이미지 설정
        billboard.setImage(textPosition, boardImage.data, boardImage.width, boardImage.height);
        
        // 5. 동일한 this.layer에 추가
        this.layer.addObject(billboard, _objectKey);
        
        return billboard;
    }
}

// 안전하게 인스턴스만 생성되어 내보내집니다 (Module 접근 안함)
export const billboardLayer = new BillboardLayer();