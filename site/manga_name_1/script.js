document.addEventListener("DOMContentLoaded", function() {
    const urlParams = new URLSearchParams(window.location.search);
    const mangaName = urlParams.get('manga');
    const mangaDetailsPath = `/content/${mangaName}/details.json`;

    // Fetch manga details
    fetch(mangaDetailsPath)
        .then(response => response.json())
        .then(details => {
            document.getElementById('manga-title').textContent = details.title;
            document.getElementById('cover-image').src = `/content/${mangaName}/cover.png`;
            document.getElementById('title').textContent = details.title;
            document.getElementById('author').textContent = `Author: ${details.author}`;
            document.getElementById('artist').textContent = `Artist: ${details.artist}`;
            document.getElementById('genres').textContent = `Genres: ${details.genres.join(', ')}`;
            document.getElementById('description').textContent = details.description;

            // Generate chapters list
            const chaptersList = document.getElementById('chapters-list');
            const chapters = Object.keys(details.chapters);
            chapters.forEach(chapter => {
                const chapterItem = document.createElement('div');
                chapterItem.innerHTML = `<a href="${mangaName}/${chapter}">${chapter}</a>`;
                chaptersList.appendChild(chapterItem);
            });
        })
        .catch(error => console.error('Error loading manga details:', error));
});
