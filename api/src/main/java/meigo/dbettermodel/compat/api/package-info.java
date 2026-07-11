/*
 * Copyright 2026 Meigo™ Corporation
 * SPDX-License-Identifier: MIT
 */
/**
 * Version-neutral abstraction over the BetterModel API.
 * <p>
 * The Denizen-facing core depends exclusively on this package; the concrete
 * {@code meigo.dbettermodel.compat.v1/v2/v3} layers implement it against
 * BetterModel 1.15.x, 2.x and 3.x respectively and are selected at runtime
 * by the bootstrap. See {@code ARCHITECTURE-6.0.md}. Interfaces land in P1.
 */
package meigo.dbettermodel.compat.api;
