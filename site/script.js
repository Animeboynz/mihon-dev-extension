document.addEventListener('DOMContentLoaded', () => {
    const contentDiv = document.getElementById('content');
    const basePath = './content';

    // Function to fetch and display all manga details
    const displayAllMangas = async () => {
        try {
            const response = await fetch(`${basePath}/mangas.json`);
            if (!response.ok) {
                throw new Error('Failed to fetch mangas.json');
            }
            const mangas = await response.json();

            mangas.forEach(mangaName => {
                displayManga(mangaName);
            });
        } catch (error) {
            console.error('Error loading mangas list:', error);
        }
    };

    // Function to fetch and display manga details
    const displayManga = async (mangaName) => {
        try {
            const response = await fetch(`${basePath}/${mangaName}/details.json`);
            if (!response.ok) {
                throw new Error(`Failed to fetch details.json for ${mangaName}`);
            }
            const details = await response.json();

            // Ensure required properties exist
            const title = details.title || 'Unknown Title';
            const author = details.author || 'Unknown Author';
            const artist = details.artist || 'Unknown Artist';
            const genres = Array.isArray(details.genres) ? details.genres.join(', ') : 'Unknown Genres';
            const description = details.description || 'No description available.';
            const chapters = Array.isArray(details.chapters) ? details.chapters : [];

            // Display the manga details
            const mangaDiv = document.createElement('div');
            mangaDiv.classList.add('manga');

            const coverImg = document.createElement('img');
            coverImg.src = `${basePath}/${mangaName}/cover.png`;
            coverImg.alt = `${title} Cover`;

            const titleElem = document.createElement('h1');
            titleElem.textContent = title;

            const authorElem = document.createElement('p');
            authorElem.textContent = `Author: ${author}`;

            const artistElem = document.createElement('p');
            artistElem.textContent = `Artist: ${artist}`;

            const genresElem = document.createElement('p');
            genresElem.textContent = `Genres: ${genres}`;

            const descriptionElem = document.createElement('p');
            descriptionElem.textContent = description;

            mangaDiv.appendChild(coverImg);
            mangaDiv.appendChild(titleElem);
            mangaDiv.appendChild(authorElem);
            mangaDiv.appendChild(artistElem);
            mangaDiv.appendChild(genresElem);
            mangaDiv.appendChild(descriptionElem);

            // Display the chapters
            const chaptersList = document.createElement('ul');
            chaptersList.classList.add('chapters-list');

            chapters.forEach((chapter) => {
                const chapterItem = document.createElement('li');
                chapterItem.textContent = chapter.title || 'Untitled Chapter';

                // Load chapter images on click
                chapterItem.addEventListener('click', () => {
                    displayChapterImages(mangaName, chapter.title);
                });

                chaptersList.appendChild(chapterItem);
            });

            mangaDiv.appendChild(chaptersList);
            contentDiv.appendChild(mangaDiv);
        } catch (error) {
            console.error('Error loading manga details:', error);
        }
    };

    // Function to display chapter images
    const displayChapterImages = async (mangaName, chapterName) => {
        try {
            const chapterPath = `${basePath}/${mangaName}/${chapterName}`;
            const chapterDiv = document.createElement('div');
            chapterDiv.classList.add('chapter-images');

            // Assume images are named sequentially
            for (let i = 1; i <= 8; i++) {
                const img = document.createElement('img');
                img.src = `${chapterPath}/${i}.png`;
                img.alt = `${chapterName} Page ${i}`;
                chapterDiv.appendChild(img);
            }

            contentDiv.innerHTML = ''; // Clear previous content
            contentDiv.appendChild(chapterDiv);
        } catch (error) {
            console.error('Error loading chapter images:', error);
        }
    };

    // Load all manga details on page load
    displayAllMangas();
});
