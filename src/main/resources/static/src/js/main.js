import { extractGpsMetadataAsync } from "./coordinateExtraction.js";

window.addEventListener("XDWorldLoaded", function () {
    console.log("🚀 XDWorld 엔진 준비 완료");

    const fileInput = document.getElementById("originalFile");
    const uploadForm = document.getElementById("uploadForm");
    const btnCreateGroup = document.getElementById("btnCreateGroup");
    
    // 느낀점 작성 관련 UI 요소
    const btnSaveMemo = document.getElementById("btnSaveMemo"); // 작성 완료 버튼
    const memoTextarea = document.getElementById("memoTextarea"); // 느낀점 입력창

    // 1. UI에서 새 그룹 생성 요청
    btnCreateGroup.addEventListener("click", async () => {
        const userId = document.getElementById("userId").value || "testUser";

        try {
            const response = await fetch("http://localhost:8080/api/images/group/start", {
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

    // 2. 파일 선택 시 EXIF 추출
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

    // 3. 이미지 업로드 전송
    uploadForm.addEventListener("submit", async (e) => {
        e.preventDefault();
        console.log("1. 폼 제출 이벤트 시작");

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
            const response = await fetch("http://localhost:8080/api/images/upload", {
                method: "POST",
                body: formData
            });

            if (!response.ok) {
                throw new Error(`업로드 실패 (HTTP ${response.status})`);
            }

            const result = await response.json();
            const imageUrl = `http://localhost:8080${result.compressedFilePath}?v=${new Date().getTime()}`;
            const targetLon = Number(lon || result.longitude || 126.9780);
            const targetLat = Number(lat || result.latitude || 37.5665);
            const targetAlt = Number(alt || result.altitude || 0.0) + 120.0;

            const objectKey = `Billboard_${result.imageId}`;
            const textObjectKey = `${objectKey}_TEXT`;
            const imagePosition = new Module.JSVector3D(targetLon, targetLat, targetAlt);

            // 이미지 업로드 성공 시 GLOBAL 상태 업데이트
            GLOBAL.selectedImageState = {
                imageId: result.imageId,
                objectKey: objectKey,
                textObjectKey: textObjectKey,
                position: imagePosition,
                memoText: ""
            };

            createIcon(`Icon_${result.imageId}`, imageUrl, function(iconName) {
                createImageBillboard(objectKey, imagePosition, iconName);
                moveCameraTo(targetLon, targetLat, targetAlt);
                console.log("🎯 이미지 빌보드가 성공적으로 생성되었습니다!");
            });

        } catch (err) {
            console.error("❌ 업로드 에러:", err);
            alert(`업로드 실패: ${err.message}`);
        }
    });

    // 4. 느낀점(Memo) 작성 완료 전송 및 텍스트 빌보드 생성
    if (btnSaveMemo) {
        btnSaveMemo.addEventListener("click", async () => {
            const selectedState = GLOBAL.selectedImageState;
            const memoText = memoTextarea ? memoTextarea.value.trim() : "";

            if (!selectedState || !selectedState.imageId) {
                alert("⚠️ 선택된 이미지가 없습니다.");
                return;
            }

            if (!memoText) {
                alert("⚠️ 느낀점을 입력해 주세요.");
                return;
            }

            try {
                // 백엔드로 느낀점 저장 요청 (@RequestParam 포맷)
                const response = await fetch("http://localhost:8080/api/images/memo", {
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

                    // 기존에 작성되어 배치된 텍스트 빌보드가 있다면 레이어에서 제거 후 재배치
                    if (GLOBAL.Layer.getObject(selectedState.textObjectKey)) {
                        GLOBAL.Layer.removeObjectByKey(selectedState.textObjectKey);
                    }

                    // 텍스트 빌보드 스타일 옵션 정의
                    const textOptions = {
                        text: memoText,
                        fontSize: 18,
                        font: "sans-serif",
                        fontColor: "#FFFFFF",
                        backgroundColor: "rgba(0, 0, 0, 0.75)",
                        outlineColor: "#000000",
                        outlineWidth: 2
                    };

                    // 지도상에 이미지 빌보드 아래 고도로 텍스트 빌보드 생성
                    createTextBillboard(
                        selectedState.textObjectKey,
                        selectedState.position,
                        textOptions
                    );

                    // 상태 업데이트 및 UI 닫기/초기화 (필요시 모달 닫기)
                    GLOBAL.selectedImageState.memoText = memoText;
                    alert("느낀점이 빌보드 아래에 성공적으로 등록되었습니다!");
                }

            } catch (err) {
                console.error("❌ 느낀점 저장 오류:", err);
                alert("느낀점 저장 중 오류가 발생했습니다.");
            }
        });
    }
});