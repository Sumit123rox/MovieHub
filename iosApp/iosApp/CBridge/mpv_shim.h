/// Minimal C function declarations for libmpv symbols needed by MpvPlayerBridge.
/// These are normally provided by mpv/client.h from the MPVKit Libmpv xcframework,
/// but if the module map isn't resolving via `import Libmpv`, these local declarations
/// let the linker find the symbols at build time.

#ifndef mpv_shim_h
#define mpv_shim_h

#include <stdio.h>
#include <stdint.h>

typedef struct mpv_handle mpv_handle;

// ── Format enum ──────────────────────────────────────────────────────────────

typedef enum mpv_format {
    MPV_FORMAT_NONE       = 0,
    MPV_FORMAT_STRING     = 1,
    MPV_FORMAT_OSD_STRING = 2,
    MPV_FORMAT_FLAG       = 3,
    MPV_FORMAT_INT64      = 4,
    MPV_FORMAT_DOUBLE     = 5,
    MPV_FORMAT_NODE       = 6,
    MPV_FORMAT_NODE_ARRAY = 7,
    MPV_FORMAT_NODE_MAP   = 8,
    MPV_FORMAT_BYTE_ARRAY = 9,
} mpv_format;

// ── Node types (for track-list, chapter-list, etc.) ──────────────────────────

typedef struct mpv_node_list {
    int num;
    struct mpv_node *values;
    char **keys;            // only used for MPV_FORMAT_NODE_MAP
} mpv_node_list;

typedef struct mpv_byte_array {
    void *data;
    size_t size;
} mpv_byte_array;

typedef struct mpv_node {
    union {
        char *string;
        int flag;
        int64_t int64;
        double double_;
        struct mpv_node_list *list;
        struct mpv_byte_array *ba;
    } u;
    mpv_format format;
} mpv_node;

// ── Event types ──────────────────────────────────────────────────────────────

typedef enum mpv_event_id {
    MPV_EVENT_NONE              = 0,
    MPV_EVENT_SHUTDOWN          = 1,
    MPV_EVENT_PROPERTY_CHANGE   = 5,
    MPV_EVENT_END_FILE          = 7,
    MPV_EVENT_FILE_LOADED       = 8,
    MPV_EVENT_AUDIO_RECONFIG    = 14,
    MPV_EVENT_VIDEO_RECONFIG    = 15,
} mpv_event_id;

typedef struct mpv_event_property {
    const char *name;
    mpv_format format;
    void *data;
} mpv_event_property;

typedef struct mpv_event {
    mpv_event_id event_id;
    int error;
    uint64_t reply_userdata;
    void *data;
} mpv_event;

// ── Core ─────────────────────────────────────────────────────────────────────

mpv_handle* mpv_create(void);
int mpv_initialize(mpv_handle *ctx);
void mpv_terminate_destroy(mpv_handle *ctx);
void mpv_free(void *data);

// ── Options ──────────────────────────────────────────────────────────────────

int mpv_set_option_string(mpv_handle *ctx, const char *name, const char *data);

// ── Commands ─────────────────────────────────────────────────────────────────

int mpv_command_string(mpv_handle *ctx, const char *args);

// ── Properties ───────────────────────────────────────────────────────────────

/// Set a property by name + typed value. format must match the value type.
int mpv_set_property(mpv_handle *ctx, const char *name, mpv_format format, void *data);

/// Convenience: set a property from its string representation.
int mpv_set_property_string(mpv_handle *ctx, const char *name, const char *data);

/// Convenience: get a property as an allocated string. Caller must mpv_free().
char *mpv_get_property_string(mpv_handle *ctx, const char *name);

// ── Properties (generic node) ────────────────────────────────────────────────

/// Retrieve a property as an mpv_node. On success, *data is filled with a
/// newly-allocated node; caller must free it with mpv_free_node_contents().
int mpv_get_property(mpv_handle *ctx, const char *name, mpv_format format, void *data);
void mpv_free_node_contents(mpv_node *node);

// ── Events ───────────────────────────────────────────────────────────────────

/// Observe a property for changes. reply_userdata is returned in the event.
int mpv_observe_property(mpv_handle *ctx, uint64_t reply_userdata,
                          const char *name, mpv_format format);

/// Wait for the next event. timeout=0 returns immediately (non-blocking).
/// Returns NULL on timeout (no event available within the timeout).
mpv_event *mpv_wait_event(mpv_handle *ctx, double timeout);

/// Set a wakeup callback that mpv calls from an internal thread when
/// new events are available. Must NOT call mpv API directly — dispatch
/// to the same queue that does mpv_wait_event instead.
void mpv_set_wakeup_callback(mpv_handle *ctx, void (*cb)(void *d), void *d);

// ── Misc ─────────────────────────────────────────────────────────────────────

const char* mpv_client_name(mpv_handle *ctx);

#endif /* mpv_shim_h */
