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

const mapUserRow = (row) => ({
  id: row.id,
  username: row.username,
  displayName: row.display_name,
  bio: row.bio || '',
  avatarUrl: row.avatar_url,
  followersCount: row.followers_count || 0,
  followingCount: row.following_count || 0,
  isFollowing: row.is_following || false,
});

// 1. GET /api/users/:id
app.get('/api/users/:id', async (req, res) => {
  try {
    const { rows } = await pool.query('SELECT * FROM users WHERE id = $1', [req.params.id]);
    if (rows.length === 0) return res.status(404).json({ error: 'User not found' });
    res.json(mapUserRow(rows[0]));
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// 2. GET /api/users/:id/friends
app.get('/api/users/:id/friends', async (req, res) => {
  try {
    const query = `
      SELECT u.* FROM users u
      INNER JOIN user_friends uf ON u.id = uf.friend_id
      WHERE uf.user_id = $1
    `;
    const { rows } = await pool.query(query, [req.params.id]);
    res.json(rows.map(mapUserRow));
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// 3. GET /api/users/:id/stats (Dashboard Obrigatório)
app.get('/api/users/:id/stats', async (req, res) => {
  try {
    const userId = req.params.id;
    const totalsQuery = `
      SELECT
        COALESCE(SUM(likes_count), 0) AS total_likes,
        COALESCE(SUM(comments_count), 0) AS total_comments,
        COUNT(*) * 25 AS total_views
      FROM recipes
      WHERE author_id = $1
    `;
    const totalsRes = await pool.query(totalsQuery, [userId]);

    const histQuery = `
      SELECT c.rating, COUNT(*) AS count
      FROM comments c
      INNER JOIN recipes r ON c.recipe_id = r.id
      WHERE r.author_id = $1 AND c.rating IS NOT NULL
      GROUP BY c.rating
    `;
    const histRes = await pool.query(histQuery, [userId]);

    const histogram = { 1: 0, 2: 0, 3: 0, 4: 0, 5: 0 };
    histRes.rows.forEach((r) => {
      histogram[r.rating] = parseInt(r.count, 10);
    });

    const t = totalsRes.rows[0];
    res.json({
      totalViews: parseInt(t.total_views, 10) || 0,
      totalLikes: parseInt(t.total_likes, 10) || 0,
      totalComments: parseInt(t.total_comments, 10) || 0,
      ratingsHistogram: histogram,
    });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

app.listen(8080, () => {
  console.log('User Service a correr no porto 8080');
});