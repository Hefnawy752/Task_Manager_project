-- Insert admin user (password = 'admin123' BCrypt encoded)
INSERT INTO users (username, email, password, created_at)
VALUES (
           'admin99',
           'admin99@taskmanager.com',
           '$2a$10$68ph173UaaGotEsXuP2lIOE2wDlTOtIQpicK/w0ZQqbujBCM4HbGO',
           NOW()
       ) ON CONFLICT DO NOTHING;

-- Insert admin role
INSERT INTO user_roles (user_id, role)
SELECT id, 'ROLE_ADMIN' FROM users WHERE username = 'admin'
    ON CONFLICT DO NOTHING;