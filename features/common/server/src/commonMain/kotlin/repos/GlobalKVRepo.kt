package space.kscience.frameswork.features.common.server.repos

import dev.inmo.micro_utils.repos.KeyValueRepo

interface GlobalKVRepo : KeyValueRepo<String, String>
