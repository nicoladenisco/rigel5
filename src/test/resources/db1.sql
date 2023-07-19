CREATE TABLE stp.transcode
(
    app VARCHAR(16) NOT NULL,
    tipo VARCHAR(16) NOT NULL,
    codice_caleido VARCHAR(64) NOT NULL,
    codice_app VARCHAR(64) NOT NULL,
    CONSTRAINT transcode_pkey PRIMARY KEY (app, tipo, codice_caleido)
);

CREATE INDEX idx_transcode_1
    ON stp.transcode (app, codice_app);
