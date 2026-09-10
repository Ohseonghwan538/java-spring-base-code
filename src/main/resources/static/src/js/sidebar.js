async function loadSidebar() {
    const response = await fetch('./src/components/sidebar.html');
    const sidebarHTML = await response.text();
    document.getElementById('sidebar').innerHTML = sidebarHTML;

    // document.getElementById('summit').addEventListener('click', () => {
    //     // 제출 버튼 클릭 시 동작할 코드 작성
    // });
}

document.addEventListener('DOMContentLoaded', loadSidebar);