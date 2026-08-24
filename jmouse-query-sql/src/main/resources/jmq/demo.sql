-- The ten worked examples' own schema. Dropped and rebuilt on every run, because a demonstration that
-- depends on what a previous run left behind stops demonstrating anything.
--
-- ⚠️ It lives in its OWN database — `jmq_demo`, created once by hand:
--
--     docker exec -i shared-mysql mysql -uroot -proot -e "
--       CREATE DATABASE IF NOT EXISTS jmq_demo CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
--       CREATE USER IF NOT EXISTS 'jmq_demo'@'%' IDENTIFIED BY 'jmq_demo';
--       GRANT ALL PRIVILEGES ON jmq_demo.* TO 'jmq_demo'@'%';"
--
-- Nothing a product owns is reachable from that user, so this file can be as destructive as it likes.
-- Every moment is written relative to NOW(), so `now() - days(7)` means something whenever it is run.

DROP TABLE IF EXISTS visits;
CREATE TABLE visits (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    patient      VARCHAR(120) NOT NULL,
    doctor_id    VARCHAR(40)  NOT NULL,
    status       VARCHAR(20)  NOT NULL,
    scheduled_at DATETIME     NOT NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

INSERT INTO visits (patient, doctor_id, status, scheduled_at) VALUES
    ('Марченко О.',  'doctor-hrytsenko', 'scheduled', NOW() - INTERVAL 4 DAY),
    ('Соколюк І.',   'doctor-hrytsenko', 'scheduled', NOW() - INTERVAL 2 DAY),
    ('Бойко Т.',     'doctor-lysenko',   'scheduled', NOW() - INTERVAL 9 HOUR),
    ('Мельник В.',   'doctor-lysenko',   'attended',  NOW() - INTERVAL 3 DAY),
    ('Кравець Н.',   'doctor-hrytsenko', 'cancelled', NOW() - INTERVAL 1 DAY),
    ('Ткаченко Ю.',  'doctor-ivanchuk',  'scheduled', NOW() - INTERVAL 6 DAY),
    ('Гончар П.',    'doctor-lysenko',   'scheduled', NOW() + INTERVAL 2 DAY),
    ('Шевчук А.',    'doctor-hrytsenko', 'scheduled', NOW() + INTERVAL 5 DAY);

DROP TABLE IF EXISTS deliveries;
CREATE TABLE deliveries (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    reference  VARCHAR(40)    NOT NULL,
    carrier    VARCHAR(40)    NOT NULL,
    city       VARCHAR(80)    NOT NULL,
    weight_kg  DECIMAL(10, 2) NOT NULL,
    shipped_at DATETIME       NOT NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

INSERT INTO deliveries (reference, carrier, city, weight_kg, shipped_at) VALUES
    ('DL-1001', 'nova-poshta', 'Київ',      12.40, NOW() - INTERVAL 2 DAY),
    ('DL-1002', 'nova-poshta', 'Львів',    340.00, NOW() - INTERVAL 5 DAY),
    ('DL-1003', 'nova-poshta', 'Одеса',    980.50, NOW() - INTERVAL 1 DAY),
    ('DL-1004', 'meest',       'Харків',   118.00, NOW() - INTERVAL 3 DAY),
    ('DL-1005', 'meest',       'Дніпро',  1240.75, NOW() - INTERVAL 8 DAY),
    ('DL-1006', 'ukrposhta',   'Полтава',    3.20, NOW() - INTERVAL 4 DAY),
    ('DL-1007', 'ukrposhta',   'Суми',     560.00, NOW() - INTERVAL 6 DAY);

DROP TABLE IF EXISTS sensor_readings;
CREATE TABLE sensor_readings (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    sensor    VARCHAR(40)    NOT NULL,
    taken_at  DATETIME       NOT NULL,
    celsius   DECIMAL(6, 2)  NOT NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- Three sensors reporting normally, one that went quiet forty minutes ago, one that has never reported
-- inside the window at all.
INSERT INTO sensor_readings (sensor, taken_at, celsius) VALUES
    ('boiler-01',  NOW() - INTERVAL  2 MINUTE, 71.20),
    ('boiler-01',  NOW() - INTERVAL 12 MINUTE, 70.80),
    ('boiler-02',  NOW() - INTERVAL  5 MINUTE, 68.10),
    ('boiler-02',  NOW() - INTERVAL 14 MINUTE, 68.40),
    ('freezer-01', NOW() - INTERVAL  9 MINUTE, -18.30),
    ('freezer-02', NOW() - INTERVAL 40 MINUTE, -17.90),
    ('freezer-02', NOW() - INTERVAL 55 MINUTE, -17.60),
    ('attic-01',   NOW() - INTERVAL  3 HOUR,    14.10);

DROP TABLE IF EXISTS subscriptions;
CREATE TABLE subscriptions (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer     VARCHAR(120)   NOT NULL,
    plan         VARCHAR(40)    NOT NULL,
    mrr          DECIMAL(10, 2) NOT NULL,
    valid_from   DATE           NOT NULL,
    valid_until  DATE           NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

INSERT INTO subscriptions (customer, plan, mrr, valid_from, valid_until) VALUES
    ('Kavovarka LLC',   'pro',        249.00, CURDATE() - INTERVAL 400 DAY, NULL),
    ('Bright Dental',   'team',       990.00, CURDATE() - INTERVAL 120 DAY, NULL),
    ('Sova Studio',     'free',         0.00, CURDATE() - INTERVAL  30 DAY, NULL),
    ('Hrim Logistics',  'enterprise', 4200.00, CURDATE() - INTERVAL 700 DAY, CURDATE() - INTERVAL 10 DAY),
    ('Vitrazh',         'pro',        249.00, CURDATE() - INTERVAL  10 DAY, CURDATE() + INTERVAL 355 DAY),
    ('Zoria Media',     'team',       990.00, CURDATE() + INTERVAL   5 DAY, NULL),
    ('Chumak & Co',     'free',         0.00, CURDATE() - INTERVAL 200 DAY, NULL);

DROP TABLE IF EXISTS deals;
CREATE TABLE deals (
    id     BIGINT AUTO_INCREMENT PRIMARY KEY,
    title  VARCHAR(120)   NOT NULL,
    owner  VARCHAR(60)    NOT NULL,
    stage  VARCHAR(40)    NOT NULL,
    amount DECIMAL(12, 2) NOT NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

INSERT INTO deals (title, owner, stage, amount) VALUES
    ('ERP для мережі аптек',  'olena',  'negotiation', 42000.00),
    ('Заміна каси',           'olena',  'negotiation',  8000.00),
    ('Річна підписка',        'olena',  'won',         31000.00),
    ('Пілот на складі',       'taras',  'qualified',    6500.00),
    ('Інтеграція з 1С',       'taras',  'negotiation', 15500.00),
    ('Міграція даних',        'taras',  'won',         27000.00),
    ('Демо для дистриб’ютора','mariia', 'qualified',    3200.00),
    ('Тендер на обладнання',  'mariia', 'negotiation', 96000.00),
    ('Дрібне доопрацювання',  'mariia', 'won',          1900.00);

DROP TABLE IF EXISTS candidates;
CREATE TABLE candidates (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    full_name  VARCHAR(120) NOT NULL,
    role       VARCHAR(120) NOT NULL,
    level      VARCHAR(20)  NOT NULL,
    status     VARCHAR(20)  NOT NULL,
    applied_at DATETIME     NOT NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

INSERT INTO candidates (full_name, role, level, status, applied_at) VALUES
    ('Ihor K.',    'Go Backend Engineer',   'senior', 'screening', NOW() - INTERVAL  2 DAY),
    ('Olha V.',    'Golang Platform',       'staff',  'new',       NOW() - INTERVAL  5 DAY),
    ('Petro M.',   'Go / Kubernetes',       'senior', 'rejected',  NOW() - INTERVAL  3 DAY),
    ('Nadiia S.',  'Go Backend Engineer',   'middle', 'new',       NOW() - INTERVAL  1 DAY),
    ('Yurii D.',   'Java Backend Engineer', 'senior', 'new',       NOW() - INTERVAL  4 DAY),
    ('Anna T.',    'Golang SRE',            'senior', 'interview', NOW() - INTERVAL 20 DAY),
    ('Roman B.',   'Go Backend Engineer',   'staff',  'interview', NOW() - INTERVAL  6 DAY);

-- ⚠️ The one schemaless example: a shop whose product attributes live in a bag, the way Innoventa's
-- fields do. `price` is TEXT in there, which is exactly why `| bigDecimal` has to be written.
DROP TABLE IF EXISTS product_attributes;
DROP TABLE IF EXISTS products;
CREATE TABLE products (
    id   BIGINT AUTO_INCREMENT PRIMARY KEY,
    sku  VARCHAR(40)  NOT NULL,
    name VARCHAR(160) NOT NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE product_attributes (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT       NOT NULL,
    attribute  VARCHAR(60)  NOT NULL,
    value      VARCHAR(255) NOT NULL,
    CONSTRAINT fk_product_attributes_product FOREIGN KEY (product_id) REFERENCES products (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

INSERT INTO products (sku, name) VALUES
    ('SKU-100', 'Кавомолка ручна'),
    ('SKU-101', 'Чайник емальований'),
    ('SKU-102', 'Френч-прес 600 мл'),
    ('SKU-103', 'Турка мідна 300 мл'),
    ('SKU-104', 'Ваги кухонні');

INSERT INTO product_attributes (product_id, attribute, value) VALUES
    (1, 'price', '890'),   (1, 'colour', 'graphite'),
    (2, 'price', '1240'),  (2, 'colour', 'ivory'),   (2, 'archived', 'yes'),
    (3, 'price', '640'),   (3, 'colour', 'clear'),
    (4, 'price', '1180'),  (4, 'colour', 'copper'),
    (5, 'price', '2350'),  (5, 'colour', 'white'),   (5, 'archived', 'yes');

DROP TABLE IF EXISTS builds;
CREATE TABLE builds (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    number           INT         NOT NULL,
    branch           VARCHAR(80) NOT NULL,
    status           VARCHAR(20) NOT NULL,
    duration_seconds INT         NOT NULL,
    finished_at      DATETIME    NOT NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

INSERT INTO builds (number, branch, status, duration_seconds, finished_at) VALUES
    (881, 'master',       'failed',  412, NOW() - INTERVAL  2 HOUR),
    (880, 'master',       'passed',  388, NOW() - INTERVAL  6 HOUR),
    (879, 'master',       'failed',  190, NOW() - INTERVAL 20 HOUR),
    (878, 'master',       'failed',  205, NOW() - INTERVAL 40 HOUR),
    (877, 'feature/jmq',  'failed',  355, NOW() - INTERVAL  3 HOUR),
    (876, 'feature/jmq',  'passed',  361, NOW() - INTERVAL  9 HOUR),
    (875, 'release/1.0',  'passed',  502, NOW() - INTERVAL 12 HOUR);

-- ⚠️ `grade` is a VARCHAR on purpose — an import nobody promised anything about. It is the column that
-- makes `| int` necessary rather than decorative: as text, "100" sorts below "60".
DROP TABLE IF EXISTS students;
CREATE TABLE students (
    id     BIGINT AUTO_INCREMENT PRIMARY KEY,
    name   VARCHAR(120) NOT NULL,
    course VARCHAR(40)  NOT NULL,
    grade  VARCHAR(10)  NOT NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

INSERT INTO students (name, course, grade) VALUES
    ('Данило Р.',  'math-101', '48'),
    ('Ірина К.',   'math-101', '100'),
    ('Остап Л.',   'math-101', '59'),
    ('Соломія Г.', 'math-101', '7'),
    ('Артем Ч.',   'math-101', '72'),
    ('Мирослав П.','phys-201', '55'),
    ('Уляна Д.',   'phys-201', '91');

DROP TABLE IF EXISTS assets;
CREATE TABLE assets (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant       VARCHAR(40)  NOT NULL,
    title        VARCHAR(160) NOT NULL,
    kind         VARCHAR(20)  NOT NULL,
    published_at DATETIME     NOT NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

INSERT INTO assets (tenant, title, kind, published_at) VALUES
    ('acme',   'Ранковий ефір, 12.08', 'video', NOW() - INTERVAL  1 DAY),
    ('acme',   'Подкаст №44',          'audio', NOW() - INTERVAL  4 DAY),
    ('acme',   'Фотозвіт з фестивалю', 'photo', NOW() - INTERVAL 30 DAY),
    ('globex', 'Інтерв’ю з мером',     'video', NOW() - INTERVAL  2 DAY),
    ('globex', 'Ранковий ефір, 12.08', 'video', NOW() - INTERVAL  3 DAY),
    ('initech','Реклама, 15 сек',      'video', NOW() - INTERVAL  5 DAY);

-- ---------------------------------------------------------------------------------------------
-- The vocabulary a real board filter needs: a row one hop away, and many rows per row.
-- ---------------------------------------------------------------------------------------------

DROP TABLE IF EXISTS candidate_skills;
DROP TABLE IF EXISTS doctors;

-- One hop: a visit points at a doctor, and a filter asks about the DOCTOR's speciality.
CREATE TABLE doctors (
    id         VARCHAR(40)  NOT NULL PRIMARY KEY,
    name       VARCHAR(120) NOT NULL,
    speciality VARCHAR(60)  NOT NULL,
    room       VARCHAR(20)  NOT NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

INSERT INTO doctors (id, name, speciality, room) VALUES
    ('doctor-hrytsenko', 'Гриценко С. П.', 'cardiology',  '204'),
    ('doctor-lysenko',   'Лисенко О. М.',  'neurology',   '311'),
    ('doctor-ivanchuk',  'Іванчук Д. В.',  'cardiology',  '207');

-- Many rows per row: a candidate has any number of skills, and no row is "the" skill.
CREATE TABLE candidate_skills (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    candidate_id BIGINT      NOT NULL,
    skill        VARCHAR(40) NOT NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

INSERT INTO candidate_skills (candidate_id, skill) VALUES
    (1, 'go'), (1, 'kubernetes'), (1, 'postgres'),
    (2, 'go'), (2, 'terraform'),
    (3, 'go'), (3, 'kubernetes'),
    (4, 'go'),
    (5, 'java'), (5, 'postgres'),
    (6, 'go'), (6, 'kubernetes'), (6, 'terraform'),
    (7, 'go'), (7, 'postgres');

-- ---------------------------------------------------------------------------------------------
-- One shape asked in two places: a level, a member one hop away, and a list of ids handed in.
-- The same rows exist as `tickets.csv`, so the identical query can be run over a parsed file.
-- ---------------------------------------------------------------------------------------------

DROP TABLE IF EXISTS tickets;
DROP TABLE IF EXISTS ticket_members;

CREATE TABLE ticket_members (
    id   VARCHAR(40)  NOT NULL PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    team VARCHAR(40)  NOT NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

INSERT INTO ticket_members (id, name, team) VALUES
    ('member-1', 'Олена К.',  'platform'),
    ('member-2', 'Тарас М.',  'platform'),
    ('member-3', 'Марія Д.',  'delivery'),
    ('member-4', 'Богдан Р.', 'delivery');

CREATE TABLE tickets (
    id        VARCHAR(40)  NOT NULL PRIMARY KEY,
    title     VARCHAR(160) NOT NULL,
    level     INT          NOT NULL,
    member_id VARCHAR(40)  NOT NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

INSERT INTO tickets (id, title, level, member_id) VALUES
    ('TCK-1', 'Каса не друкує чек',      3, 'member-1'),
    ('TCK-2', 'Повільний пошук',         2, 'member-2'),
    ('TCK-3', 'Дрібна опечатка',         1, 'member-3'),
    ('TCK-4', 'Втрата даних при імпорті',3, 'member-3'),
    ('TCK-5', 'Кнопка не там',           1, 'member-4'),
    ('TCK-6', 'Дубль у звіті',           2, 'member-4');
