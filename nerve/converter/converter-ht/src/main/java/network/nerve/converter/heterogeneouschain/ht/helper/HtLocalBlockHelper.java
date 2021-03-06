/**
 * MIT License
 * <p>
 * Copyright (c) 2019-2020 nerve.network
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package network.nerve.converter.heterogeneouschain.ht.helper;

import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import network.nerve.converter.heterogeneouschain.ht.model.HtSimpleBlockHeader;
import network.nerve.converter.heterogeneouschain.ht.storage.HtBlockHeaderStorageService;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 本地HT区块操作器
 *
 * @author: Mimi
 * @date: 2020-02-20
 */
@Component
public class HtLocalBlockHelper {

    @Autowired
    private HtBlockHeaderStorageService htBlockStorageService;
    private static final String BLOCK_HEADER_KEY = "LOCAL_BLOCK_HEADER";
    private static final ConcurrentHashMap<String, HtSimpleBlockHeader> localBlockHeaderMaps = new ConcurrentHashMap<>();

    public HtSimpleBlockHeader getLatestLocalBlockHeader() {
        HtSimpleBlockHeader localBlockHeader = localBlockHeaderMaps.get(BLOCK_HEADER_KEY);
        if (localBlockHeader == null) {
            localBlockHeader = this.findLatest();
            if(localBlockHeader != null) {
                localBlockHeaderMaps.putIfAbsent(BLOCK_HEADER_KEY, localBlockHeader);
            }
        }
        return localBlockHeader;
    }

    /**
     * 保存本地最新区块
     */
    public void saveLocalBlockHeader(HtSimpleBlockHeader blockHeader) throws Exception {
        localBlockHeaderMaps.put(BLOCK_HEADER_KEY, blockHeader);
        htBlockStorageService.save(blockHeader);
    }

    /**
     * 查询本地数据库中最新区块
     */
    private HtSimpleBlockHeader findLatest() {
        HtSimpleBlockHeader blockHeader = htBlockStorageService.findLatest();
        return blockHeader;
    }

    /**
     * 根据高度查询区块
     */
    public HtSimpleBlockHeader findByHeight(long height) {
        return htBlockStorageService.findByHeight(height);
    }

    public void deleteByHeight(Long localBlockHeight) throws Exception {
        htBlockStorageService.deleteByHeight(localBlockHeight);
    }

    public void deleteByHeightAndUpdateMemory(Long localBlockHeight) throws Exception {
        this.deleteByHeight(localBlockHeight);
        // 缓存上一区块为本地最新区块
        HtSimpleBlockHeader blockHeader = findByHeight(localBlockHeight - 1);
        if (blockHeader != null) {
            localBlockHeaderMaps.put(BLOCK_HEADER_KEY, blockHeader);
        } else {
            localBlockHeaderMaps.remove(BLOCK_HEADER_KEY);
        }
    }

    public void deleteAllLocalBlockHeader() throws Exception {
        HtSimpleBlockHeader localBlockHeader = this.getLatestLocalBlockHeader();
        while(localBlockHeader != null) {
            this.deleteByHeightAndUpdateMemory(localBlockHeader.getHeight());
            localBlockHeader = this.getLatestLocalBlockHeader();
        }
    }
}
