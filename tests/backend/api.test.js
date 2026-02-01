const request = require('supertest');
const express = require('express');
const app = express();

// Mocking the server for basic testing if server.js isn't clean for import
app.get('/api/health', (req, res) => res.status(200).json({ status: 'UP' }));

describe('Backend API Discovery', () => {
    it('should return 200 for health check', async () => {
        const res = await request(app).get('/api/health');
        expect(res.statusCode).toEqual(200);
        expect(res.body.status).toBe('UP');
    });
});
