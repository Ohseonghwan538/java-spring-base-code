// 레이어 관리
import { tileLayer } from "./layers/TileLayer.js";
import { billboardLayer } from "./layers/user/BillboardLayer.js";

// 이미지 좌표 추출 모듈
import { extractGpsMetadataAsync } from "./coordinateExtraction.js";

// 헬퍼 함수 관리
import { createIcon, moveCameraTo, createPositionWithOffset } from "../utils/helperMethod.js";

window.addEventListener("XDWorldLoaded", function () {
    console.log("🚀 XDWorld 엔진 준비 완료");

    // 레이어 초기화
    tileLayer.init();
    billboardLayer.init();

    let selectedImageState = {
        imageId: null,
        objectKey: null,
        textObjectKey: null,
        position: { longitude: 0, latitude: 0, altitude: 0 },
        textPosition: { longitude: 0, latitude: 0, altitude: 0 },
        memoText: ""
    };

    const fileInput = document.getElementById("originalFile");
    const uploadForm = document.getElementById("uploadForm");
    const btnCreateGroup = document.getElementById("btnCreateGroup");
    const btnLoadImages = document.getElementById("btnLoadImages");
    const apiBaseUrl = window.location.origin;
    const imageMemos = new Map();
    
    const memoModal = document.getElementById("memoModal");
    const btnSaveMemo = document.getElementById("btnSaveMemo");
    const memoTextarea = document.getElementById("memoTextarea");

    // =========================================================================
    // 올바른 XDWorld 3D 객체 클릭(선택) 이벤트 등록 (Fire_EventSelectedObject)
    // =========================================================================
    Module.canvas.addEventListener("Fire_EventSelectedObject", function (e) {
        if (!e || !e.objKey) return;

        // 선택된 오브젝트 키 추출
        const objectKey = e.objKey; 
        console.log("🖱️ 3D 객체 선택됨 (Key):", objectKey);

        // 이미지 빌보드 조건 검사 (Billboard_로 시작하고 _TEXT가 아닌 객체)
        if (objectKey.startsWith("Billboard_") && !objectKey.endsWith("_TEXT")) {
            const imageId = objectKey.replace("Billboard_", "");
            
            // 선택된 객체 가져오기
            const selectedObject = billboardLayer.layer.getObjects(objectKey);
            if (!selectedObject) return;

            const position = selectedObject.getPosition(); // JSVector3D 형태
            const textPosition = createPositionWithOffset(position, -18.0);

            // 선택 상태 업데이트
            selectedImageState.imageId = Number(imageId);
            selectedImageState.objectKey = objectKey;
            selectedImageState.textObjectKey = `${objectKey}_TEXT`;
            selectedImageState.position = position;
            selectedImageState.textPosition = textPosition;
            selectedImageState.memoText = imageMemos.get(Number(imageId)) || "";

            console.log("✅ 선택된 이미지 상태 바인딩 완료:", selectedImageState);

            // UI 모달/입력창 활성화
            if (memoModal) memoModal.style.display = "block";
            if (memoTextarea) {
                memoTextarea.value = selectedImageState.memoText;
                memoTextarea.focus();
            }
        }
    });

    // 1. 새 그룹 생성
    btnCreateGroup.addEventListener("click", async () => {
        const userId = document.getElementById("userId").value || "testUser";

        try {
            const response = await fetch(`${apiBaseUrl}/api/images/group/start`, {
                method: "POST",
                headers: { "Content-Type": "application/x-www-form-urlencoded" },
                body: new URLSearchParams({ userId: userId })
            });

            if (!response.ok) throw new Error("그룹 생성 실패");

            const data = await response.json();
            document.getElementById("groupId").value = data.groupId;
            alert(`새 그룹이 생성되었습니다! (Group ID: ${data.groupId})`);

        } catch (err) {
            console.error("❌ 그룹 생성 오류:", err);
            alert("그룹 생성 중 오류가 발생했습니다.");
        }
    });

    // 2. 사용자 ID와 그룹 ID에 저장된 이미지 불러오기
    btnLoadImages.addEventListener("click", async () => {
        const userId = document.getElementById("userId").value.trim();
        const groupId = document.getElementById("groupId").value;

        if (!userId || !groupId) {
            alert("⚠️ 사용자 ID와 그룹 ID를 모두 입력해 주세요.");
            return;
        }

        try {
            const query = new URLSearchParams({ userId, groupId });
            const response = await fetch(`${apiBaseUrl}/api/images?${query}`);
            if (!response.ok) throw new Error(`이미지 불러오기 실패 (HTTP ${response.status})`);

            const data = await response.json();
            if (data.images.length === 0) {
                alert("저장된 이미지가 없습니다. 사용자 ID와 그룹 ID를 확인해 주세요.");
                return;
            }

            data.images.forEach((image, index) => renderStoredImage(image, index === 0));
            alert(`${data.images.length}개의 이미지를 불러왔습니다.`);
        } catch (err) {
            console.error("❌ 이미지 불러오기 오류:", err);
            alert(`이미지 불러오기 중 오류가 발생했습니다: ${err.message}`);
        }
    });

    // 3. EXIF 좌표 추출
    fileInput.addEventListener("change", async (e) => {
        const file = e.target.files[0];
        if (!file) return;

        const gpsData = await extractGpsMetadataAsync(file);
        if (gpsData) {
            document.getElementById("longitude").value = gpsData.lon;
            document.getElementById("latitude").value = gpsData.lat;
            document.getElementById("altitude").value = gpsData.alt;
        }
    });

    // 4. 이미지 업로드 전송 및 빌보드 생성
    uploadForm.addEventListener("submit", async (e) => {
        e.preventDefault();

        const groupId = document.getElementById("groupId").value;
        const fileInput = document.getElementById("originalFile");

        if (!groupId) {
            alert("⚠️ 그룹 ID가 없습니다. '새 그룹 생성' 버튼을 먼저 눌러주세요.");
            return;
        }

        if (!fileInput.files[0]) {
            alert("⚠️ 업로드할 파일을 선택해 주세요.");
            return;
        }

        const formData = new FormData();
        formData.append("groupId", groupId);
        formData.append("originalFile", fileInput.files[0]);

        const lon = document.getElementById("longitude").value;
        const lat = document.getElementById("latitude").value;
        const alt = document.getElementById("altitude").value;

        if (lon && lon.trim() !== "") formData.append("longitude", lon);
        if (lat && lat.trim() !== "") formData.append("latitude", lat);
        if (alt && alt.trim() !== "") formData.append("altitude", alt);

        try {
            const response = await fetch(`${apiBaseUrl}/api/images/upload`, {
                method: "POST",
                body: formData
            });

            if (!response.ok) throw new Error(`업로드 실패 (HTTP ${response.status})`);

            const result = await response.json();
            const imageUrl = `${apiBaseUrl}${result.compressedFilePath}?v=${new Date().getTime()}`;
            const targetLon = Number(lon || result.longitude || 126.9780);
            const targetLat = Number(lat || result.latitude || 37.5665);
            const targetAlt = Number(alt || result.altitude || 0.0) + 120.0;

            const objectKey = `Billboard_${result.imageId}`;
            const textObjectKey = `${objectKey}_TEXT`;
            const imagePosition = new Module.JSVector3D(targetLon, targetLat, targetAlt);

            // 선택 상태 설정
            selectedImageState.imageId = result.imageId;
            selectedImageState.objectKey = objectKey;
            selectedImageState.textObjectKey = textObjectKey;
            selectedImageState.position = imagePosition;
            selectedImageState.textPosition = createPositionWithOffset(imagePosition, -18.0);
            selectedImageState.memoText = "";
            imageMemos.set(result.imageId, "");

            createIcon(`Icon_${result.imageId}`, imageUrl, function(iconName) {
                billboardLayer.createImageBillboard(objectKey, imagePosition, iconName);
                moveCameraTo(targetLon, targetLat, targetAlt);
                console.log("🎯 이미지 빌보드 등록 완료!");
            });

        } catch (err) {
            console.error("❌ 업로드 에러:", err);
            alert(`업로드 실패: ${err.message}`);
        }
    });

    // 5. 느낀점(Memo) 저장 및 텍스트 빌보드 추가
    if (btnSaveMemo) {
        btnSaveMemo.addEventListener("click", async () => {
            const selectedState = selectedImageState;
            const memoText = memoTextarea ? memoTextarea.value.trim() : "";

            if (!selectedState || !selectedState.imageId) {
                alert("⚠️ 선택된 이미지 빌보드가 없습니다. 지도의 빌보드를 먼저 클릭해 주세요.");
                return;
            }

            if (!memoText) {
                alert("⚠️ 느낀점을 입력해 주세요.");
                return;
            }

            try {
                const response = await fetch(`${apiBaseUrl}/api/images/memo`, {
                    method: "POST",
                    headers: { "Content-Type": "application/x-www-form-urlencoded" },
                    body: new URLSearchParams({
                        imageId: selectedState.imageId,
                        memo: memoText
                    })
                });

                if (!response.ok) throw new Error("느낀점 저장 실패");

                const data = await response.json();

                if (data.status === "SUCCESS") {
                    console.log("📝 느낀점 저장 완료:", data.memo);

                    if (billboardLayer.layer && billboardLayer.layer.getObjects(selectedState.textObjectKey)) {
                        billboardLayer.layer.removeAtKey(selectedState.textObjectKey);
                    }

                    const textOptions = {
                        text: memoText,
                        fontSize: 18,
                        font: "sans-serif",
                        fontColor: "#FFFFFF",
                        backgroundColor: "rgba(0, 0, 0, 0.75)",
                        outlineColor: "#000000",
                        outlineWidth: 2
                    };

                    billboardLayer.billboardLayer.createTextBillboard(
                        selectedState.textObjectKey,
                        selectedState.position,
                        textOptions
                    );

                    selectedImageState.memoText = memoText;
                    imageMemos.set(selectedState.imageId, memoText);
                    if (memoModal) memoModal.style.display = "none";
                    alert("느낀점이 빌보드 아래에 성공적으로 등록되었습니다!");
                }

            } catch (err) {
                console.error("❌ 느낀점 저장 오류:", err);
                alert("느낀점 저장 중 오류가 발생했습니다.");
            }
        });
    }

    function renderStoredImage(image, moveCamera) {
        const longitude = Number(image.longitude);
        const latitude = Number(image.latitude);
        const altitude = Number(image.altitude || 0) + 120.0;

        if (!Number.isFinite(longitude) || !Number.isFinite(latitude)) {
            console.warn("좌표가 없는 이미지는 지도에 표시할 수 없습니다.", image.imageId);
            return;
        }

        const objectKey = `Billboard_${image.imageId}`;
        const textObjectKey = `${objectKey}_TEXT`;
        const position = new Module.JSVector3D(longitude, latitude, altitude);
        const imageUrl = `${apiBaseUrl}${image.compressedFilePath}?v=${Date.now()}`;

        imageMemos.set(image.imageId, image.memo || "");

        if (billboardLayer.layer.getObjects(objectKey)) billboardLayer.layer.removeAtKey(objectKey);
        if (billboardLayer.layer.getObjects(textObjectKey)) billboardLayer.layer.removeAtKey(textObjectKey);

        createIcon(`Icon_${image.imageId}`, imageUrl, (iconName) => {
            billboardLayer.createImageBillboard(objectKey, position, iconName);

            if (image.memo) {
                billboardLayer.createTextBillboard(textObjectKey, position, createMemoTextOptions(image.memo));
            }
            if (moveCamera) moveCameraTo(longitude, latitude, altitude);
        });
    }

    function createMemoTextOptions(memoText) {
        return {
            text: memoText,
            fontSize: 18,
            font: "sans-serif",
            fontColor: "#FFFFFF",
            backgroundColor: "rgba(0, 0, 0, 0.75)",
            outlineColor: "#000000",
            outlineWidth: 2
        };
    }

    // 선택 상태 초기화 함수
    function resetSelectedImageState() {
        this.selectedImageState.imageId = null;
        this.selectedImageState.objectKey = null;
        this.selectedImageState.textObjectKey = null;
        this.selectedImageState.position = { longitude: 0, latitude: 0, altitude: 0 };
        this.selectedImageState.textPosition = { longitude: 0, latitude: 0, altitude: 0 };
        this.selectedImageState.memoText = "";
    };
});
