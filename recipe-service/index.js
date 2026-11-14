const express = require('express');
const cors = require('cors');
const { Pool } = require('pg');

const app = express();
app.use(cors());
app.use(express.json());

const pool = new Pool({
  host: process.env.DB_HOST || 'cooktok-db',
  user: process.env.DB_USER || 'admin',
  password: process.env.DB_PASSWORD || 'secretpassword',
  database: process.env.DB_NAME || 'cooktok_server_db',
  port: 5432,
});

const mapRecipeRow = (row) => ({
  id: row.id,
  title: row.title,
  description: row.description,
  authorId: row.author_id,
  authorName: row.author_name,
  videoUrl: row.video_url,
  photoUrl: row.photo_url,
  ingredients: row.ingredients || [],
  steps: row.steps || [],
  durationMinutes: row.duration_minutes || 0,
  portions: row.portions || 1,
  difficulty: row.difficulty || 'MEDIUM',
  curiosities: row.curiosities,
  locationName: row.location_name,
  latitude: row.latitude,
  longitude: row.longitude,
  likesCount: row.likes_count || 0,
  commentsCount: row.comments_count || 0,
  averageRating: parseFloat(row.average_rating) || 0.0,
  isLiked: row.is_liked || false,
  isFavorite: row.is_favorite || false,
  createdAt: parseInt(row.created_at, 10) || Date.now(),
});

// 1. GET /api/recipes/feed?filter=recent|popular|rating
app.get('/api/recipes/feed', async (req, res) => {
  try {
    const filter = req.query.filter || 'recent';
    let orderBy = 'created_at DESC';
    if (filter === 'popular') orderBy = 'likes_count DESC';
    if (filter === 'rating') orderBy = 'average_rating DESC';

    const { rows } = await pool.query(`SELECT * FROM recipes ORDER BY ${orderBy}`);
    res.json(rows.map(mapRecipeRow));
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// 2. POST /api/recipes (Criar nova receita)
app.post('/api/recipes', async (req, res) => {
  try {
    const r = req.body;
    const id = r.id || `rec_${Date.now()}`;
    const createdAt = r.createdAt || Date.now();

    const query = `
      INSERT INTO recipes (
        id, title, description, author_id, author_name, video_url, photo_url,
        ingredients, steps, duration_minutes, portions, difficulty,
        curiosities, location_name, latitude, longitude,
        likes_count, comments_count, average_rating, is_liked, is_favorite, created_at
      ) VALUES (
        $1, $2, $3, $4, $5, $6, $7, $8::jsonb, $9::jsonb, $10, $11, $12,
        $13, $14, $15, $16, $17, $18, $19, $20, $21, $22
      )
      ON CONFLICT (id) DO UPDATE SET
        title = EXCLUDED.title,
        description = EXCLUDED.description
      RETURNING *
    `;

    const values = [
      id, r.title, r.description, r.authorId || 'user_1', r.authorName || 'Chef Bruno',
      r.videoUrl || null, r.photoUrl || null,
      JSON.stringify(r.ingredients || []), JSON.stringify(r.steps || []),
      r.durationMinutes || 0, r.portions || 1, r.difficulty || 'MEDIUM',
      r.curiosities || null, r.locationName || null, r.latitude || null, r.longitude || null,
      r.likesCount || 0, r.commentsCount || 0, r.averageRating || 0.0,
      r.isLiked || false, r.isFavorite || false, createdAt
    ];

    const { rows } = await pool.query(query, values);
    res.status(201).json(mapRecipeRow(rows[0]));
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// 3. POST /api/recipes/search (Filtro de Inventário por Ingredientes)
app.post('/api/recipes/search', async (req, res) => {
  try {
    const searchTerms = (req.body.ingredients || []).map((i) => i.toLowerCase().trim());
    const { rows } = await pool.query('SELECT * FROM recipes ORDER BY created_at DESC');
    const allRecipes = rows.map(mapRecipeRow);

    if (searchTerms.length === 0) return res.json(allRecipes);

    const filtered = allRecipes.filter((recipe) => {
      const recipeIngNames = (recipe.ingredients || []).map((ing) => (ing.name || '').toLowerCase());
      return searchTerms.every((term) =>
        recipeIngNames.some((rIng) => rIng.includes(term)) ||
        recipe.title.toLowerCase().includes(term)
      );
    });

    res.json(filtered);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// 4. POST /api/recipes/:id/like
app.post('/api/recipes/:id/like', async (req, res) => {
  try {
    const { rows } = await pool.query('SELECT * FROM recipes WHERE id = $1', [req.params.id]);
    if (rows.length === 0) return res.status(404).json({ error: 'Recipe not found' });

    const current = rows[0];
    const newLiked = !current.is_liked;
    const newCount = newLiked ? current.likes_count + 1 : Math.max(0, current.likes_count - 1);

    const updated = await pool.query(
      'UPDATE recipes SET is_liked = $1, likes_count = $2 WHERE id = $3 RETURNING *',
      [newLiked, newCount, req.params.id]
    );
    res.json(mapRecipeRow(updated.rows[0]));
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// 5. GET /api/recipes/:id/comments
app.get('/api/recipes/:id/comments', async (req, res) => {
  try {
    const { rows } = await pool.query(
      'SELECT * FROM comments WHERE recipe_id = $1 ORDER BY created_at DESC',
      [req.params.id]
    );
    res.json(
      rows.map((c) => ({
        id: c.id,
        recipeId: c.recipe_id,
        authorId: c.author_id,
        authorName: c.author_name,
        authorAvatarUrl: c.author_avatar_url,
        text: c.text,
        rating: c.rating,
        createdAt: parseInt(c.created_at, 10) || Date.now(),
      }))
    );
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// 6. POST /api/recipes/:id/comments
app.post('/api/recipes/:id/comments', async (req, res) => {
  try {
    const recipeId = req.params.id;
    const c = req.body;
    const id = c.id || `com_${Date.now()}`;
    const createdAt = c.createdAt || Date.now();

    const insertQuery = `
      INSERT INTO comments (id, recipe_id, author_id, author_name, author_avatar_url, text, rating, created_at)
      VALUES ($1, $2, $3, $4, $5, $6, $7, $8)
      RETURNING *
    `;
    const { rows } = await pool.query(insertQuery, [
      id, recipeId, c.authorId || 'user_1', c.authorName || 'Chef Bruno',
      c.authorAvatarUrl || null, c.text, c.rating || null, createdAt,
    ]);

    await pool.query(
      `UPDATE recipes SET
        comments_count = comments_count + 1,
        average_rating = COALESCE((SELECT AVG(rating) FROM comments WHERE recipe_id = $1 AND rating IS NOT NULL), average_rating)
       WHERE id = $1`,
      [recipeId]
    );

    const saved = rows[0];
    res.status(201).json({
      id: saved.id,
      recipeId: saved.recipe_id,
      authorId: saved.author_id,
      authorName: saved.author_name,
      authorAvatarUrl: saved.author_avatar_url,
      text: saved.text,
      rating: saved.rating,
      createdAt: parseInt(saved.created_at, 10),
    });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

app.listen(8080, () => {
  console.log('Recipe Service a correr no porto 8080');
});