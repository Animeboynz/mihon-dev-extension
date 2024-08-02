document.addEventListener("DOMContentLoaded", function() {
    const urlParams = new URLSearchParams(window.location.search);
    const mangaName = urlParams.get('manga');
    const chapterName = urlParams.get('chapter');
    const chapterPath = `/`;

    document.getElementById('chapter-title').textContent = `${mangaName} - ${chapterName}`;

    // Generate chapter images
    const chapterImages = document.getElementById('chapter-images');
    fetch(chapterPath)
        .then(response => response.json())
        .then(images => {
            images.forEach(image => {
                const img = document.createElement('img');
                img.src = `${chapterPath}/${image}`;
                chapterImages.appendChild(img);
            });
        })
        .catch(error => console.error('Error loading chapter images:', error));
});
