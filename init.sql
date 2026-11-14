-- 1. Tabela de Utilizadores
CREATE TABLE IF NOT EXISTS users (
    id VARCHAR(50) PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    display_name VARCHAR(150) NOT NULL,
    bio TEXT DEFAULT '',
    avatar_url TEXT,
    followers_count INT DEFAULT 0,
    following_count INT DEFAULT 0,
    is_following BOOLEAN DEFAULT FALSE
);

-- 2. Tabela de Amigos / Seguidores
CREATE TABLE IF NOT EXISTS user_friends (
    user_id VARCHAR(50) REFERENCES users(id) ON DELETE CASCADE,
    friend_id VARCHAR(50) REFERENCES users(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, friend_id)
);

-- 3. Tabela de Receitas (CookToks)
CREATE TABLE IF NOT EXISTS recipes (
    id VARCHAR(50) PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    description TEXT NOT NULL,
    author_id VARCHAR(50) REFERENCES users(id) ON DELETE CASCADE,
    author_name VARCHAR(150) NOT NULL,
    video_url TEXT,
    photo_url TEXT,
    ingredients JSONB DEFAULT '[]'::jsonb,
    steps JSONB DEFAULT '[]'::jsonb,
    duration_minutes INT DEFAULT 0,
    portions INT DEFAULT 1,
    difficulty VARCHAR(20) DEFAULT 'MEDIUM',
    curiosities TEXT,
    location_name VARCHAR(150),
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    likes_count INT DEFAULT 0,
    comments_count INT DEFAULT 0,
    average_rating REAL DEFAULT 0.0,
    is_liked BOOLEAN DEFAULT FALSE,
    is_favorite BOOLEAN DEFAULT FALSE,
    created_at BIGINT NOT NULL
);

-- 4. Tabela de Comentários e Avaliações
CREATE TABLE IF NOT EXISTS comments (
    id VARCHAR(50) PRIMARY KEY,
    recipe_id VARCHAR(50) REFERENCES recipes(id) ON DELETE CASCADE,
    author_id VARCHAR(50) REFERENCES users(id) ON DELETE CASCADE,
    author_name VARCHAR(150) NOT NULL,
    author_avatar_url TEXT,
    text TEXT NOT NULL,
    rating INT,
    created_at BIGINT NOT NULL
);

-- ==========================================
-- DADOS INICIAIS DE TESTE (SEED DATA)
-- ==========================================

INSERT INTO users (id, username, display_name, bio, avatar_url, followers_count, following_count, is_following)
VALUES
    ('user_1', 'chef_bruno', 'Chef Bruno', 'Receitas rápidas para estudantes!', NULL, 120, 45, false),
    ('user_2', 'maria_cook', 'Maria Silva', 'Fã de massas e doces tradicionais.', NULL, 85, 90, true)
ON CONFLICT (id) DO NOTHING;

INSERT INTO user_friends (user_id, friend_id)
VALUES ('user_1', 'user_2')
ON CONFLICT DO NOTHING;

INSERT INTO recipes (
    id, title, description, author_id, author_name, video_url, photo_url,
    ingredients, steps, duration_minutes, portions, difficulty,
    curiosities, location_name, latitude, longitude,
    likes_count, comments_count, average_rating, is_liked, is_favorite, created_at
)
VALUES
(
    'rec_1',
    'Frango com Arroz Cremoso',
    'Perfeito para aproveitar sobras do frigorífico em 20 minutos.',
    'user_1',
    'Chef Bruno',
    'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4',
    'https://images.unsplash.com/photo-1604908176997-125f25cc6f3d',
    '[{"id":"ing_1","name":"Frango","quantity":"300","unit":"g"},{"id":"ing_2","name":"Arroz","quantity":"200","unit":"g"},{"id":"ing_3","name":"Alho","quantity":"2","unit":"dentes"}]'::jsonb,
    '["Cortar o frango em cubos e dourar com alho.", "Juntar o arroz e envolver bem.", "Adicionar água a ferver e cozinhar 15 minutos."]'::jsonb,
    20, 2, 'EASY',
    'Prato muito popular nas cozinhas universitárias.',
    'Porto, Portugal', 41.1579, -8.6291,
    15, 1, 4.5, false, false, 1731540000000
),
(
    'rec_2',
    'Massa Carbonara Autêntica',
    'Sem natas! Apenas ovos, queijo pecorino e guanciale.',
    'user_2',
    'Maria Silva',
    'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4',
    'https://images.unsplash.com/photo-1612874742237-6526221588e3',
    '[{"id":"ing_4","name":"Esparguete","quantity":"250","unit":"g"},{"id":"ing_5","name":"Ovos","quantity":"3","unit":"un"},{"id":"ing_6","name":"Bacon","quantity":"150","unit":"g"}]'::jsonb,
    '["Cozer a massa em água e sal.", "Fritar o bacon até ficar estaladiço.", "Misturar as gemas com queijo e juntar à massa fora do lume."]'::jsonb,
    15, 2, 'MEDIUM',
    'Originária da região do Lácio, em Itália.',
    'Roma, Itália', 41.9028, 12.4964,
    32, 1, 5.0, true, true, 1731543600000
)
ON CONFLICT (id) DO NOTHING;

INSERT INTO comments (id, recipe_id, author_id, author_name, author_avatar_url, text, rating, created_at)
VALUES
    ('com_1', 'rec_1', 'user_2', 'Maria Silva', NULL, 'Fiz ontem ao jantar e ficou incrível!', 5, 1731541000000),
    ('com_2', 'rec_2', 'user_1', 'Chef Bruno', NULL, 'Finalmente uma receita sem natas!', 5, 1731544000000)
ON CONFLICT (id) DO NOTHING;