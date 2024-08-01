document.addEventListener('DOMContentLoaded', function () {
    const mangaGrid = document.getElementById('manga-grid');

    fetch('mangas.json')
        .then(response => response.json())
        .then(mangaList => {
            mangaList.forEach(manga => {
                const mangaItem = document.createElement('div');
                mangaItem.classList.add('manga-item');

                const mangaLink = document.createElement('a');
                mangaLink.href = `${manga.folderName}.html`;
                mangaLink.classList.add('manga-link');

                const mangaImage = document.createElement('img');
                mangaImage.src = `${manga.folderName}/cover.png`;
                mangaImage.alt = `${manga.displayName} cover image`;

                const mangaName = document.createElement('div');
                mangaName.classList.add('manga-name');
                mangaName.textContent = manga.displayName;

                mangaLink.appendChild(mangaImage);
                mangaLink.appendChild(mangaName);
                mangaItem.appendChild(mangaLink);

                mangaGrid.appendChild(mangaItem);
            });
        })
        .catch(error => console.error('Error fetching manga list:', error));
});
