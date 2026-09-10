const apiBaseUrl = window.location.origin;

export const imageApi = {
    // 새 그룹 생성
    async createGroup(userId = "testUser") {
        const response = await fetch(`${apiBaseUrl}/api/images/group/start`, {
            method: "POST",
            headers: { "Content-Type": "application/x-www-form-urlencoded" },
            body: new URLSearchParams({ userId })
        });
        if (!response.ok) throw new Error("그룹 생성 실패");
        return response.json();
    },

    // 이미지 목록 조회
    async fetchImages(userId, groupId) {
        const query = new URLSearchParams({ userId, groupId });
        const response = await fetch(`${apiBaseUrl}/api/images?${query}`);
        if (!response.ok) throw new Error(`이미지 불러오기 실패 (HTTP ${response.status})`);
        return response.json();
    },

    // 이미지 파일 업로드
    async uploadImage(formData) {
        const response = await fetch(`${apiBaseUrl}/api/images/upload`, {
            method: "POST",
            body: formData
        });
        if (!response.ok) throw new Error(`업로드 실패 (HTTP ${response.status})`);
        return response.json();
    },

    // 메모(느낀점) 저장
    async saveMemo(imageId, memo) {
        const response = await fetch(`${apiBaseUrl}/api/images/memo`, {
            method: "POST",
            headers: { "Content-Type": "application/x-www-form-urlencoded" },
            body: new URLSearchParams({ imageId, memo })
        });
        if (!response.ok) throw new Error("느낀점 저장 실패");
        return response.json();
    }
};