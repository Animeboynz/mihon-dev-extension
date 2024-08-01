document.addEventListener('DOMContentLoaded', () => {
    fetch('manga_name_1/details.json')
        .then(response => response.json())
        .then(data => {
            document.getElementById('cover-image').src = `manga_name_1/cover.png`;
            document.getElementById('title').textContent = data.title;
            document.getElementById('author').textContent = data.author;
            document.getElementById('artist').textContent = data.artist;
            document.getElementById('genres').textContent = data.genres.join(', ');
            document.getElementById('description').textContent = data.description;

            const chapterList = document.getElementById('chapter-list');
            data.chapters.forEach(chapter => {
                const listItem = document.createElement('li');
                const link = document.createElement('a');
                link.href = `mangareader.com/manga_name_1/${chapter.folder}.html`;
                link.textContent = chapter.title;
                listItem.appendChild(link);
                chapterList.appendChild(listItem);
            });
        })
        .catch(error => console.error('Error fetching manga details:', error));
});
